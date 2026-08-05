package com.bashkevich.tennisscorekeeperbackend.feature.theme

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.model.executeStructured
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.AttachmentSource
import com.bashkevich.tennisscorekeeperbackend.model.theme.AiLayoutResult
import com.bashkevich.tennisscorekeeperbackend.model.theme.ThemeContent
import com.bashkevich.tennisscorekeeperbackend.model.theme.ThemeDto
import com.bashkevich.tennisscorekeeperbackend.model.theme.toDto
import com.bashkevich.tennisscorekeeperbackend.plugins.LLMException
import com.bashkevich.tennisscorekeeperbackend.plugins.WrongEntityException
import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery
import io.ktor.http.ContentType
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray

class ThemeService(
    private val themeRepository: ThemeRepository,
) {
    /**
     * Koog prompt executor (OpenAI). Создаётся лениво — только при первом обращении к /themes/ai,
     * поэтому отсутствие OPENAI_API_KEY не ломает остальные роуты тем (getThemes/create/...).
     * Ключ читается из окружения по первому использованию.
     */
    private val promptExecutor: PromptExecutor by lazy {
        MultiLLMPromptExecutor(
            OpenAILLMClient(System.getenv("OPENAI_API_KEY") ?: error("OPENAI_API_KEY is not set"))
        )
    }

    suspend fun getThemes(): List<ThemeDto> {
        return dbQuery {
            themeRepository.getAll().map { it.toDto() }
        }
    }

    suspend fun getThemeById(themeId: Int): ThemeDto {
        return dbQuery {
            if (themeId == 0) throw BadRequestException("Incorrect id")
            themeRepository.getById(themeId)?.toDto()
                ?: throw NotFoundException("No theme found!")
        }
    }

    suspend fun createTheme(name: String, content: ThemeContent): ThemeDto {
        return dbQuery {
            themeRepository.create(name, content).toDto()
        }
    }

    suspend fun updateTheme(id: Int, name: String, content: ThemeContent): ThemeDto {
        return dbQuery {
            if (id == 0) throw BadRequestException("Incorrect id")
            themeRepository.update(id, name, content)?.toDto()
                ?: throw NotFoundException("No theme found!")
        }
    }

    /**
     * Генерирует тему табло из загруженного изображения через AI (Koog, OpenAI GPT-4o).
     *
     * Двухфазный конвейер (см. план): LLM только **локализует** элементы табло, размечая
     * ячейки сетки ролями, а детерминированный движок измеряет точный `#RRGGBB` по реальным
     * пикселям. Поэтому «чёрный вместо тёмно-синего» исчезает по построению — модель вообще
     * не называет цвета.
     *
     * Выполняется БЕЗ dbQuery — это чистый LLM-вызов, не должен удерживать DB-соединение.
     * На этапе отладки тема только возвращается, в БД не сохраняется.
     *
     * Поведение:
     * - изображение не является табло → [WrongEntityException] (HTTP 422);
     * - нет картинки / не image-тип → [BadRequestException] (HTTP 400);
     * - технический сбой AI / неразборчивая разметка → [LLMException] (HTTP 500).
     */
    suspend fun generateThemeFromImage(fileData: MultiPartData): ThemeContent {
        val image = readImageFromMultipart(fileData)

        // Фаза 1 — локализация. Рисуем поверх картинки фиксированную пронумерованную сетку
        // и просим GPT-4o размечать ячейки ролями (grid-техника — надёжный grounding-паттерн
        // для vision-модели, не требующий точных координат bounding box'ов).
        val overlay = ScoreboardColorExtractor.drawGridOverlay(
            image.bytes, AiLayoutResult.GRID_ROWS, AiLayoutResult.GRID_COLS
        )

        val layoutPrompt = prompt("scoreboard_layout") {
            system(LAYOUT_PROMPT)
            user {
                +"Analyze the attached image. It has an overlaid ${AiLayoutResult.GRID_ROWS}x${AiLayoutResult.GRID_COLS} numbered grid."
                +"Rows are numbered top-to-bottom, columns left-to-right."
                +"If it is a tennis scoreboard, set is_scoreboard=true and return the grid of cell roles."
                +"If it is NOT a tennis scoreboard, set is_scoreboard=false and provide a short reason."
                image(
                    AttachmentSource.Image(
                        content = AttachmentContent.Binary.Bytes(overlay),
                        format = "png",
                        mimeType = "image/png",
                        fileName = "scoreboard-grid.png",
                    )
                )
            }
        }.withUpdatedParams { temperature = 0.0 }

        val result = promptExecutor.executeStructured<AiLayoutResult>(
            prompt = layoutPrompt,
            model = OpenAIModels.Chat.GPT4o,
        )

        val structured = result.getOrElse { error ->
            throw LLMException("AI failed to analyze the image", error)
        }

        if (!structured.data.isScoreboard) {
            throw WrongEntityException(structured.data.reason ?: "Image is not a tennis scoreboard")
        }

        validateGrid(structured.data.grid)

        // Фаза 2 — измерение. Точный цвет по реальным пикселям ИСХОДНОЙ картинки (без сетки).
        return try {
            ScoreboardColorExtractor.extract(image.bytes, structured.data.grid)
        } catch (e: IllegalStateException) {
            throw LLMException("AI layout could not be resolved into colors: ${e.message}", e)
        }
    }

    /**
     * Проверяет, что вернувшаяся от LLM матрица ролей имеет ожидаемую размерность
     * [AiLayoutResult.GRID_ROWS]×[AiLayoutResult.GRID_COLS]. Иначе модель не поняла grid
     * и измерять цвета небезопасно — это технический сбой, а не «не табло».
     */
    private fun validateGrid(grid: List<List<String>>) {
        val expectedRows = AiLayoutResult.GRID_ROWS
        val expectedCols = AiLayoutResult.GRID_COLS
        if (grid.size != expectedRows) {
            throw LLMException("AI returned grid with ${grid.size} rows, expected $expectedRows")
        }
        grid.forEachIndexed { rowIndex, row ->
            if (row.size != expectedCols) {
                throw LLMException("AI returned row $rowIndex with ${row.size} cols, expected $expectedCols")
            }
        }
    }

    /**
     * Описывает текстом содержимое табло по загруженному изображению через AI (Koog, OpenAI GPT-4o).
     *
     * promptExecutor возвращает обычную строку (plain-text выполнение через [PromptExecutor.execute],
     * без structured-обёртки): игроки, кто подаёт, текущий счёт (завершённые сеты, текущий сет,
     * текущий гейм), расположение лиц игроков и ключевые цвета табло (название + #RRGGBB).
     * Описание — на русском. HTTP-слой оборачивает строку в JSON (ResponseMessageDto).
     *
     * Выполняется БЕЗ dbQuery — это чистый LLM-вызов.
     *
     * Поведение:
     * - изображение не является табло → строка «На изображении не теннисное табло.»
     *   (модель сама возвращает эту фразу по инструкции в промпте);
     * - нет картинки / не image-тип → [BadRequestException] (HTTP 400);
     * - технический сбой AI → RuntimeException (HTTP 500).
     */
    suspend fun describeMatchFromImage(fileData: MultiPartData): String {
        val image = readImageFromMultipart(fileData)

        val descriptionPrompt = prompt("scoreboard_match_description") {
            system(DESCRIBE_MATCH_PROMPT)
            user {
                +"Опиши изображение теннисного табло на русском языке."
                +"Если это не теннисное табло — ответь ровно фразой: На изображении не теннисное табло."
                image(
                    AttachmentSource.Image(
                        content = AttachmentContent.Binary.Bytes(image.bytes),
                        format = image.format,
                        mimeType = image.mimeType,
                        fileName = image.fileName,
                    )
                )
            }
        }.withUpdatedParams { temperature = 0.0 }

        val response = try {
            promptExecutor.execute(descriptionPrompt, OpenAIModels.Chat.GPT4o)
        } catch (error: Exception) {
            throw RuntimeException("AI failed to analyze the image: ${error.message}", error)
        }

        return response.textContent().trim().ifBlank { "На изображении не теннисное табло." }
    }

    private suspend fun readImageFromMultipart(fileData: MultiPartData): ImageInput {
        var bytes: ByteArray? = null
        var contentType: ContentType? = null
        var originalFileName: String? = null

        fileData.forEachPart { part ->
            when (part) {
                is PartData.FileItem -> {
                    if (part.name == "image") {
                        val partContentType = part.contentType
                        if (partContentType != null && partContentType.match(ContentType.Image.Any)) {
                            bytes = part.provider().readRemaining().readByteArray()
                            contentType = partContentType
                            originalFileName = part.originalFileName
                        } else {
                            throw BadRequestException(
                                "Expected an image file (image/*), got content type: $partContentType"
                            )
                        }
                    }
                }

                else -> {}
            }
            part.release()
        }

        val imageBytes = bytes
            ?: throw BadRequestException(
                "No image file found in request. Use 'image' as the multipart field name and an image/* content type."
            )
        val partContentType = contentType!!

        val format = partContentType.toString()
            .substringAfter("image/", missingDelimiterValue = "")
            .lowercase()
            .ifEmpty { "png" }

        return ImageInput(
            bytes = imageBytes,
            format = format,
            mimeType = partContentType.toString(),
            fileName = originalFileName ?: "scoreboard.$format",
        )
    }

    private data class ImageInput(
        val bytes: ByteArray,
        val format: String,
        val mimeType: String,
        val fileName: String,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ImageInput

            if (!bytes.contentEquals(other.bytes)) return false
            if (format != other.format) return false
            if (mimeType != other.mimeType) return false
            if (fileName != other.fileName) return false

            return true
        }

        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + format.hashCode()
            result = 31 * result + mimeType.hashCode()
            result = 31 * result + fileName.hashCode()
            return result
        }
    }
}

private val LAYOUT_PROMPT = """
    You are an expert at analyzing tennis scoreboards. You receive exactly one image with a numbered
    grid overlaid on it. Your ONLY job is to decide whether the image is a tennis scoreboard and, if so,
    label each grid cell with the UI role it shows. You must NEVER name, estimate or guess colors —
    a deterministic engine measures colors from the real pixels afterwards.

    The image is divided into a fixed grid of 8 rows x 20 columns. Cell [row][col] is addressed with
    row 0 at the top and column 0 at the left. Return "grid" as a list of 8 lists, each containing
    exactly 20 strings (one role per cell). Every cell gets exactly one role.

    Available roles (use these exact lowercase strings):
      - "background": plain empty background of the scoreboard panel (the base fill behind everything).
      - "name_text": a player's name or regular score label (the normal text of the board).
      - "prev_set_win_text": text of a completed-set score that this player WON (often brighter/normal).
      - "prev_set_lose_text": text of a completed-set score that this player LOST (often dimmed/grey).
      - "current_set_bg": the highlighted fill/background of the cell showing the CURRENT set score.
      - "current_set_text": the text inside that current-set cell.
      - "current_game_bg": the highlighted fill/background of the cell showing the CURRENT game/points score.
      - "current_game_text": the text inside that current-game cell.
      - "serve": the serve indicator next to one player's name — a small ball/dot, asterisk or dash
        marking WHO is serving. It usually sits immediately left or right of the serving player's name.
      - "ignore": anything that is not part of the scoreboard (player photos/faces, logos, TV channel
        graphics, the area around the board, people, scenery).

    How to label:
      - A tennis scoreboard typically has two player rows (one above the other) and columns: name,
        completed-set scores, current-set score, current-game/points score. One or both of the
        current-set / current-game cells may be highlighted with a distinct fill.
      - Each cell gets exactly ONE role. If a cell straddles two elements, pick the one occupying the
        larger share of that cell.
      - Mark the serve indicator cell with "serve" (not "name_text"). If you cannot see any serve
        indicator, do not emit "serve" anywhere — leave those cells as their underlying role.
      - Mark cells that show regular names/scores (not inside a highlighted current-set/current-game
        cell) as "name_text", and won/lost completed sets as "prev_set_win_text" / "prev_set_lose_text"
        where the win/lose distinction is visually clear; otherwise use "name_text".
      - Cells fully outside the board (faces, logos, surrounding area) must be "ignore".

    If the image IS a tennis scoreboard, return:
        "is_scoreboard": true,
        "grid": <8x20 matrix of role strings as described above>,
        "reason": null

    If the image is NOT a tennis scoreboard (e.g. a photo of a person, animal, scenery, a different sport,
    a logo, a screenshot of text, etc.), return:
        "is_scoreboard": false,
        "grid": <8x20 matrix; you may fill it entirely with "ignore">,
        "reason": <a short explanation>

    The grid MUST always be present and be exactly 8 rows x 20 columns, regardless of is_scoreboard.
    Return ONLY the structured result — no explanations, no markdown.
""".trimIndent()

private val DESCRIBE_MATCH_PROMPT = """
    You are an expert at analyzing tennis scoreboards. You receive a single image.
    Decide whether it is a tennis scoreboard.

    Respond with PLAIN TEXT ONLY, in Russian, without any markdown, code blocks, or extra commentary.

    If the image is NOT a tennis scoreboard (e.g. a photo of a person, animal, scenery,
    a different sport, a logo, a screenshot of text, etc.), respond with exactly this sentence
    and nothing else:
    На изображении не теннисное табло.

    If the image IS a tennis scoreboard, write a clear, concise plain-text description IN RUSSIAN
    that covers:

    - Игроки: имена, если они видны на табло; иначе укажи позицию каждого (верхний/нижний
      или левый/правый).
    - Кто подаёт: явно укажи, какой из игроков сейчас подаёт (если индикатор подачи виден).
    - Счёт:
        * завершённые сеты — по игрокам (например, 1:6, 6:4);
        * текущий сет — счёт в идущем сете (например, 3:2);
        * текущий гейм — счёт очков в текущем гейме (например, 30:15).
    - Лица игроков: где расположены фотографии/лица игроков на табло (верх/низ или лево/право),
      если они есть на изображении.
    - Цвета: перечисли ВСЕ цвета табло, что требует /themes/ai (ровно этот набор),
      каждый в виде "название #RRGGBB (словесное описание цвета)". Значения #RRGGBB бери точно с изображения:
        * основной цвет фона
        * основной цвет текста
        * цвет индикатора подачи (какой игрок подаёт)
        * цвет текста у выигравшего прошедший сет
        * цвет текста у проигравшего прошедший сет
        * цвет фона текущего сета
        * цвет текста текущего сета
        * цвет фона текущего гейма
        * цвет текста текущего гейма

    Return ONLY the resulting text — no explanations, no markdown.
""".trimIndent()
