package com.bashkevich.tennisscorekeeperbackend.model.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ LLM (GPT-5.4) на этапе **классификации** палитры — фаза 2 конвейера `/themes/ai`.
 *
 * Модель получает картинку табло + палитру (hex + частота), и для КАЖДОГО слота темы выбирает
 * один hex из палитры. Поля цветов — **non-null и без дефолтов**: так JSON-схема помечает их
 * `required`, и Koog принуждает модель заполнить все слоты (см. инструкцию в промпте
 * «Pick a hex for EVERY slot — never return null»). Это явное требование конвейера: ни один
 * цвет не остаётся неклассифицированным.
 *
 * Намеренно плоская структура (не sealed on_success/on_failure) по той же причине, что и
 * [AiThemeExtractionResult]: OpenAI-генератор JSON-схемы в Koog требует обычный класс на корне,
 * иначе падает с `Key $ref is missing in the map`. Поле [isScoreboard] даёт модели легальный
 * способ отказаться от обработки в рамках structured output (422 на не-табло).
 *
 * `@SerialName` каждого поля зеркалит соответствующее поле [ThemeContent].
 */
@Serializable
data class PaletteClassification(
    @SerialName("is_scoreboard") val isScoreboard: Boolean,
    @SerialName("reason") val reason: String? = null,
    @SerialName("main_background_color") val mainBackgroundColor: String,
    @SerialName("main_text_color") val mainTextColor: String,
    @SerialName("serve_color") val serveColor: String,
    @SerialName("previous_set_background_color") val previousSetBackgroundColor: String,
    @SerialName("previous_set_win_text_color") val previousSetWinTextColor: String,
    @SerialName("previous_set_lose_text_color") val previousSetLoseTextColor: String,
    @SerialName("current_set_background_color") val currentSetBackgroundColor: String,
    @SerialName("current_set_text_color") val currentSetTextColor: String,
    @SerialName("current_game_background_color") val currentGameBackgroundColor: String,
    @SerialName("current_game_text_color") val currentGameTextColor: String,
)
