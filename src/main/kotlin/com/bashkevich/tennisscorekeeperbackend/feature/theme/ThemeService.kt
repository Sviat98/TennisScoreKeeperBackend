package com.bashkevich.tennisscorekeeperbackend.feature.theme

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.model.executeStructured
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.AttachmentSource
import com.bashkevich.tennisscorekeeperbackend.model.theme.AiThemeExtractionResult
import com.bashkevich.tennisscorekeeperbackend.model.theme.MatchDescriptionResult
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
     * Выполняется БЕЗ dbQuery — это чистый LLM-вызов, не должен удерживать DB-соединение.
     * На этапе отладки тема только возвращается, в БД не сохраняется.
     *
     * Поведение:
     * - изображение не является табло → [WrongEntityException] (HTTP 422);
     * - нет картинки / не image-тип → [BadRequestException] (HTTP 400);
     * - технический сбой AI → [LLMException] (HTTP 500).
     */
    suspend fun generateThemeFromImage(fileData: MultiPartData): ThemeContent {
        val image = readImageFromMultipart(fileData)

        val extractionPrompt = prompt("scoreboard_theme_extraction") {
            system(SYSTEM_PROMPT)
            user {
                +"Analyze the attached image."
                +"If it is a tennis scoreboard, extract its color scheme: set is_scoreboard=true and put it in theme."
                +"If it is NOT a tennis scoreboard: set is_scoreboard=false and provide a short reason."
                image(
                    AttachmentSource.Image(
                        content = AttachmentContent.Binary.Bytes(image.bytes),
                        format = image.format,
                        mimeType = image.mimeType,
                        fileName = image.fileName,
                    )
                )
            }
        }

        val result = promptExecutor.executeStructured<AiThemeExtractionResult>(
            prompt = extractionPrompt,
            model = OpenAIModels.Chat.GPT4o,
        )

        val structured = result.getOrElse { error ->
            throw LLMException("AI failed to analyze the image", error)
        }

        return when {
            structured.data.isScoreboard -> structured.data.theme
                ?: throw LLMException("AI flagged the image as a scoreboard but returned no theme")

            else -> throw WrongEntityException(structured.data.reason ?: "Image is not a tennis scoreboard")
        }
    }

    /**
     * Описывает текстом содержимое табло по загруженному изображению через AI (Koog, OpenAI GPT-4o).
     *
     * Возвращает обычную строку: игроки, кто подаёт, текущий счёт (завершённые сеты, текущий сет,
     * текущий гейм), расположение лиц игроков и ключевые цвета табло (название + #RRGGBB).
     * Описание — на русском.
     *
     * Выполняется БЕЗ dbQuery — это чистый LLM-вызов.
     *
     * Поведение:
     * - изображение не является табло → строка «На изображении не теннисное табло.»;
     * - нет картинки / не image-тип → [BadRequestException] (HTTP 400);
     * - технический сбой AI → RuntimeException (HTTP 500).
     */
    suspend fun describeMatchFromImage(fileData: MultiPartData): String {
        val image = readImageFromMultipart(fileData)

        val descriptionPrompt = prompt("scoreboard_match_description") {
            system(DESCRIBE_MATCH_PROMPT)
            user {
                +"Опиши изображение теннисного табло на русском языке."
                +"Если это не теннисное табло — верни on_failure с короткой причиной."
                image(
                    AttachmentSource.Image(
                        content = AttachmentContent.Binary.Bytes(image.bytes),
                        format = image.format,
                        mimeType = image.mimeType,
                        fileName = image.fileName,
                    )
                )
            }
        }

        val result = promptExecutor.executeStructured<MatchDescriptionResult>(
            prompt = descriptionPrompt,
            model = OpenAIModels.Chat.GPT4o,
        )

        val structured = result.getOrElse { error ->
            throw RuntimeException("AI failed to analyze the image: ${error.message}", error)
        }

        return when (val data = structured.data) {
            is MatchDescriptionResult.OnSuccess -> data.description
            is MatchDescriptionResult.OnFailure -> "На изображении не теннисное табло."
        }
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

private val SYSTEM_PROMPT = """
    You are an expert at analyzing tennis scoreboards and extracting their exact color scheme.
    You receive a single image. Decide whether it is a tennis scoreboard.

    If the image IS a tennis scoreboard:
        "is_scoreboard": true,
        "theme": an object with these nine colors, each {"color": "<#RRGGBB hex>", "alpha": <0.0-1.0, default 1.0>}:
          - main_background_color: the overall background of the scoreboard.
          - main_text_color: the default text color (player names, main scores).
          - serve_color: the color/indicator marking which player is serving.
          - previous_set_win_text_color: the text color of set counts the player has WON (previous sets).
          - previous_set_lose_text_color: the text color of set counts the player has LOST (previous sets).
          - current_set_background_color: the background highlighting the CURRENT set column.
          - current_set_text_color: the text color inside the current set highlight.
          - current_game_background_color: the background highlighting the CURRENT game score.
          - current_game_text_color: the text color inside the current game highlight.

    If the image is NOT a tennis scoreboard (e.g. a photo of a person, animal, scenery,
      a different sport, a logo, a screenshot of text, etc.):
        "is_scoreboard": false,
        "reason": a short explanation of why it is not a scoreboard.

    Use accurate hex colors sampled from the image. Prefer alpha = 1.0 unless a color is clearly semi-transparent.
    Return ONLY the structured result — no explanations, no markdown.
""".trimIndent()

private val DESCRIBE_MATCH_PROMPT = """
    You are an expert at analyzing tennis scoreboards. You receive a single image.
    Decide whether it is a tennis scoreboard.

    If the image is NOT a tennis scoreboard (e.g. a photo of a person, animal, scenery,
    a different sport, a logo, a screenshot of text, etc.), return "on_failure" with a short "reason".

    If the image IS a tennis scoreboard, return "on_success" with a "description": a clear, concise
    plain-text description IN RUSSIAN (no markdown, no extra commentary) that covers:

    - Игроки: имена, если они видны на табло; иначе укажи позицию каждого (верхний/нижний
      или левый/правый).
    - Кто подаёт: явно укажи, какой из игроков сейчас подаёт (если индикатор подачи виден).
    - Счёт:
        * завершённые сеты — по игрокам (например, 1:6, 6:4);
        * текущий сет — счёт в идущем сете (например, 3:2);
        * текущий гейм — счёт очков в текущем гейме (например, 30:15).
    - Лица игроков: где расположены фотографии/лица игроков на табло (верх/низ или лево/право),
      если они есть на изображении.
    - Цвета: перечисли ключевые цвета табло, каждый в виде "название #RRGGBB (словесное описание цвета)".
      Минимум эти цвета: фон табло, основной текст, индикатор подачи, подсветка текущего сета,
      подсветка текущего гейма. Значения #RRGGBB бери точно с изображения.

    Верни ТОЛЬКО структурированный результат — без пояснений и markdown.
""".trimIndent()
