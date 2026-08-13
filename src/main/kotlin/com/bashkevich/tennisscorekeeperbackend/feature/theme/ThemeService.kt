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
import com.bashkevich.tennisscorekeeperbackend.model.theme.PaletteClassification
import com.bashkevich.tennisscorekeeperbackend.model.theme.ThemeColor
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
     * Генерирует тему табло из загруженного изображения через AI (Koog, OpenAI GPT-5.4).
     *
     * Двухфазный конвейер «палитра → классификация» (см. план и референс ScoreboardThemeRecognizer):
     *   1. [PaletteExtractor.extract] детерминированно считает палитру цветов изображения + частоту
     *      каждого (Color Thief MMCQ + частотный проход по пикселям).
     *   2. LLM получает картинку И таблицу «hex → %», и классифицирует, какой цвет к какому слоту
     *      темы относится (текущий гейм, текущий сет, предыдущий сет и т.д.) — [PaletteClassification].
     * После ответа каждый hex привязывается к ближайшему цвету палитры ([snapToPalette]), чтобы
     * опечатка модели не породила цвет вне палитры.
     *
     * Выполняется БЕЗ dbQuery — это чистый LLM-вызов, не должен удерживать DB-соединение.
     * На этапе отладки тема только возвращается, в БД не сохраняется.
     *
     * Поведение:
     * - изображение не является табло → [WrongEntityException] (HTTP 422);
     * - нет картинки / не image-тип / не извлеклась палитра → [BadRequestException] / [LLMException];
     * - технический сбой AI → [LLMException] (HTTP 500).
     */
    suspend fun generateThemeFromImage(fileData: MultiPartData): ThemeContent {
        val image = readImageFromMultipart(fileData)

        // Фаза 1 — палитра с частотами. Color Thief (MMCQ) + проход по пикселям.
        val palette = PaletteExtractor.extract(image.bytes)
        if (palette.isEmpty()) {
            throw LLMException("Could not extract color palette from the image")
        }

        // Картинка для LLM — единый PNG (модель видит ровно то, из чего считалась палитра).
        val analyzedPng = PaletteExtractor.encodeToPng(image.bytes)

        val paletteText = buildString {
            append("| Hex | Frequency |\n")
            append("|---|---|\n")
            palette.forEach { c ->
                append("| `")
                append(c.centroid.toHex())
                append("` | ")
                append("%.1f%%".format(c.share * 100))
                append(" |\n")
            }
        }

        // Фаза 2 — классификация. И картинка, и таблица палитры; temperature у GPT-5.4 фиксированная,
        // override не нужен.
        val classificationPrompt = prompt("scoreboard_palette_roles") {
            system(PALETTE_ROLES_PROMPT)
            user {
                +"The attached image is the scoreboard. Its color palette as a"
                +"markdown table (hex + frequency %):"
                +paletteText
                +"Pick a hex for EVERY slot from the table above — never return null."
                image(
                    AttachmentSource.Image(
                        content = AttachmentContent.Binary.Bytes(analyzedPng),
                        format = "png",
                        mimeType = "image/png",
                        fileName = "scoreboard-palette.png",
                    )
                )
            }
        }

        val result = promptExecutor.executeStructured<PaletteClassification>(
            prompt = classificationPrompt,
            model = OpenAIModels.Chat.GPT5_4,
        )

        val structured = result.getOrElse { error ->
            throw LLMException("AI failed to classify the palette", error)
        }

        if (!structured.data.isScoreboard) {
            throw WrongEntityException(structured.data.reason ?: "Image is not a tennis scoreboard")
        }

        val snapped = snapToPalette(structured.data, palette)
        return toThemeContent(snapped)
    }

    /**
     * Прежняя (до палитры) реализация /themes/ai: один LLM-вызов, в котором GPT-5.4 сам решает,
     * табло ли это, и сам называет все цвета (через [AiThemeExtractionResult]). Оставлена как
     * fallback-эндпоинт `/themes/ai/old`: палитра иногда схлопывает близкие оттенки, а эта версия
     * работает с чистым изображением и выбирает цвета напрямую.
     *
     * Поведение:
     * - не табло → [WrongEntityException] (HTTP 422);
     * - нет картинки / не image-тип → [BadRequestException] (HTTP 400);
     * - технический сбой AI → [LLMException] (HTTP 500).
     */
    suspend fun generateThemeFromImageLegacy(fileData: MultiPartData): ThemeContent {
        val image = readImageFromMultipart(fileData)

        val extractionPrompt = prompt("scoreboard_theme_extraction") {
            system(LEGACY_SYSTEM_PROMPT)
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
            model = OpenAIModels.Chat.GPT5_4,
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

/**
 * Привязывает каждый hex из ответа LLM к ближайшему цвету палитры, чтобы классификация ссылалась
 * только на реальные цвета палитры (защита от «близкого, но не точного» hex от модели). Непарсимый
 * hex (в принципе невозможен по промпту, но защитно) сваливается на самый частый цвет палитры.
 */
private fun snapToPalette(raw: PaletteClassification, palette: List<ClusterInfo>): PaletteClassification {
    val fallbackHex = palette.first().centroid.toHex()
    fun snap(hex: String): String {
        val c = parseHexColor(hex) ?: return fallbackHex
        return palette.minByOrNull { it.centroid.distanceTo(c) }?.centroid?.toHex() ?: fallbackHex
    }
    return PaletteClassification(
        isScoreboard = raw.isScoreboard,
        reason = raw.reason,
        mainBackgroundColor = snap(raw.mainBackgroundColor),
        mainTextColor = snap(raw.mainTextColor),
        serveColor = snap(raw.serveColor),
        previousSetWinTextColor = snap(raw.previousSetWinTextColor),
        previousSetLoseTextColor = snap(raw.previousSetLoseTextColor),
        previousSetBackgroundColor = snap(raw.previousSetBackgroundColor),
        currentSetBackgroundColor = snap(raw.currentSetBackgroundColor),
        currentSetTextColor = snap(raw.currentSetTextColor),
        currentGameBackgroundColor = snap(raw.currentGameBackgroundColor),
        currentGameTextColor = snap(raw.currentGameTextColor),
    )
}

/** Сборка [ThemeContent] из классификации (все hex уже snapped, alpha = 1.0). */
private fun toThemeContent(c: PaletteClassification): ThemeContent = ThemeContent(
    mainBackgroundColor = ThemeColor(c.mainBackgroundColor),
    mainTextColor = ThemeColor(c.mainTextColor),
    serveColor = ThemeColor(c.serveColor),
    previousSetWinTextColor = ThemeColor(c.previousSetWinTextColor),
    previousSetLoseTextColor = ThemeColor(c.previousSetLoseTextColor),
    previousSetBackgroundColor = ThemeColor(c.previousSetBackgroundColor),
    currentSetBackgroundColor = ThemeColor(c.currentSetBackgroundColor),
    currentSetTextColor = ThemeColor(c.currentSetTextColor),
    currentGameBackgroundColor = ThemeColor(c.currentGameBackgroundColor),
    currentGameTextColor = ThemeColor(c.currentGameTextColor),
)

private val PALETTE_ROLES_PROMPT = """
    # Tennis scoreboard — palette → theme slots

    You receive ONE scoreboard **image** plus its **color palette** as a markdown
    table of `hex` → `frequency %`. Frequency is only a hint: backgrounds dominate it, text and
    accents are rarer — do *not* rank by frequency alone.

    First decide whether the image is a tennis scoreboard (look at the image, not the palette).

    ## Task
    If it IS a scoreboard, for each slot below choose the **single hex** from the palette that best
    matches what fills that slot **on the image**. Several slots may share the same hex.

    ## Slots

    | Slot | What it is |
    |---|---|
    | `main_background_color` | dominant fill behind the player names — the board's base color |
    | `main_text_color` | player SURNAME text color |
    | `serve_color` | serve indicator (small ball/dot), often an accent |
    | `previous_set_background_color` | the fill behind a COMPLETED (previous) set's score cell — usually the same as the board's base color, but some boards do highlight previous sets with their own fill |
    | `previous_set_win_text_color` | a COMPLETED set's digit for the player who won it |
    | `previous_set_lose_text_color` | the same completed set's digit for the loser — usually the dimmer/grayer version of the win text |
    | `current_set_background_color` | fill of the highlighted cell showing the CURRENT set score |
    | `current_set_text_color` | the digit on that current-set cell |
    | `current_game_background_color` | fill of the highlighted cell showing the CURRENT game/points |
    | `current_game_text_color` | the digit on that current-game cell |

    ## Common patterns (typical, NOT strict rules — trust the image first)
    - **`current_set` and `current_game` cells usually have *different* colors** — both their
      backgrounds and their text. Only copy one cell's colors to the other if the image really shows
      them identical.
    - **`serve_color` and `previous_set_win_text_color` usually *coincide*** — both are typically
      the board's bright accent (e.g. yellow). Pick the same hex for both when that matches the image.
    - **`previous_set_background_color` usually *equals* `main_background_color`** — previous sets
      are not highlighted on most boards. Pick the same hex for both when the image shows no special
      fill behind previous sets; only pick a different hex if the image clearly highlights them.
    - Text slots may still all share one hex (e.g. all white); the patterns above are only about
      cell distinctness and accent/background sharing.

    ## Rules
    - Return a hex for **every** slot — never empty. If a slot is unclear or not clearly visible,
      still pick the single best-guess hex from the palette. A missing / dash answer is never acceptable.
    - Return ONLY hexes that appear **verbatim** in the provided palette.
    - Return ONLY the structured result.

    If the image IS a tennis scoreboard, return:
        "is_scoreboard": true,
        the ten slots above, each a hex string from the palette,
        "reason": null

    If the image is NOT a tennis scoreboard (e.g. a photo of a person, animal, scenery, a different
    sport, a logo, a screenshot of text, etc.), return:
        "is_scoreboard": false,
        "reason": <a short explanation>
        (still fill every slot with any hex from the palette — they are ignored in this case)
""".trimIndent()

private val LEGACY_SYSTEM_PROMPT = """
    You are an expert at analyzing tennis scoreboards and extracting their exact color scheme.
    You receive exactly one image. Determine whether the image contains a tennis scoreboard.

    IMPORTANT - color extraction rules:
    - Extract colors directly from the visible pixels.
    - Do NOT estimate, normalize, beautify or adjust colors.
    - Do NOT return typical or expected tennis scoreboard colors. Return the colors actually visible in this image.
    - For each requested element, identify the correct UI element first, then determine its dominant visible color.
    - If anti-aliasing, gradients or compression artifacts are present, choose the dominant visible color.
    - Ignore shadows, borders, reflections and decorative effects whenever possible.
    - Ignore text when determining background colors.
    - Ignore background when determining text colors.
    - Always return exactly one dominant color for every requested field.
    - Prefer alpha = 1.0 unless a color is clearly semi-transparent.

    Missing elements (fallback) - every field is required:
    - If a UI element is NOT visible on the image, do not leave its field empty. Derive its color from the
      colors you DID detect, so the result stays consistent and readable:
        * Missing text-type color (serve_color, previous_set_win_text_color, previous_set_lose_text_color,
          current_set_text_color, current_game_text_color) -> reuse main_text_color.
        * Missing fill/background color (previous_set_background_color, current_set_background_color,
          current_game_background_color) -> reuse main_background_color.
    - Rationale: an absent highlight cell should look like the rest of the board (base colors);
      an absent text element should match the main text.
    - Examples: no visible serve indicator -> serve_color = main_text_color;
      no current game shown -> current_game_background_color = main_background_color and
      current_game_text_color = main_text_color.

    If the image IS a tennis scoreboard, return:
        "is_scoreboard": true,
        "theme": an object with these ten colors, each {"color": "<#RRGGBB hex>", "alpha": <0.0-1.0, default 1.0>}:
          - main_background_color: dominant background color of the scoreboard itself.
          - main_text_color: dominant color of regular player names and score text.
          - serve_color: color of the serve indicator (fallback = main_text_color if absent).
          - previous_set_background_color: fill behind a completed set's score cell (fallback = main_background_color if absent; usually equals main_background_color unless previous sets are highlighted).
          - previous_set_win_text_color: text color of completed sets won (fallback = main_text_color if absent).
          - previous_set_lose_text_color: text color of completed sets lost (fallback = main_text_color if absent).
          - current_set_background_color: dominant fill color of the highlighted current-set cell (fallback = main_background_color if absent).
          - current_set_text_color: text color inside the highlighted current-set cell (fallback = main_text_color if absent).
          - current_game_background_color: dominant fill color of the highlighted current-game cell (fallback = main_background_color if absent).
          - current_game_text_color: text color inside the highlighted current-game cell (fallback = main_text_color if absent).

    If the image is NOT a tennis scoreboard (e.g. a photo of a person, animal, scenery,
      a different sport, a logo, a screenshot of text, etc.), return:
        "is_scoreboard": false,
        "reason": a short explanation of why it is not a scoreboard.

    Return ONLY the structured result - no explanations, no markdown.
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
        * цвет фона предыдущего сета (обычно совпадает с основным фоном, если прошедшие сеты не подсвечены)
        * цвет текста у выигравшего прошедший сет
        * цвет текста у проигравшего прошедший сет
        * цвет фона текущего сета
        * цвет текста текущего сета
        * цвет фона текущего гейма
        * цвет текста текущего гейма

    Return ONLY the resulting text — no explanations, no markdown.
""".trimIndent()
