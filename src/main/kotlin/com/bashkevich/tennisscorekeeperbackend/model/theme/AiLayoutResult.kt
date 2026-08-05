package com.bashkevich.tennisscorekeeperbackend.model.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ LLM на этапе **локализации** табло (фаза 1 конвейера /themes/ai).
 *
 * Модель **не называет цвета** — она только размечает ячейки наложенной сетки
 * [GRID_ROWS]×[GRID_COLS] ролями (см. `ScoreboardColorExtractor.Role`). Точный цвет
 * затем измеряется детерминированным движком по реальным пикселям. Поэтому «чёрный
 * вместо тёмно-синего» исчезает по построению: у модели просто нет способа исказить цвет.
 *
 * Намеренно плоская структура (не sealed on_success/on_failure) по той же причине, что и
 * [AiThemeExtractionResult]: OpenAI-генератор JSON-схемы в Koog требует обычный класс на
 * корне, иначе падает с `Key $ref is missing in the map`. Поле [isScoreboard] даёт модели
 * легальный способ отказаться от обработки в рамках structured output.
 *
 * Роли — строковые значения (а не enum), перечисленные и валидируемые на сервере: enum в
 * JSON-схему намеренно не кладём, чтобы не нараться на особенности генератора схем Koog.
 *
 * Семантика полей и список ролей описаны в system-промпте ThemeService.LAYOUT_PROMPT,
 * поэтому сама модель остаётся без знаний о внутреннем устройстве.
 */
@Serializable
data class AiLayoutResult(
    @SerialName("is_scoreboard") val isScoreboard: Boolean,
    @SerialName("reason") val reason: String? = null,
    @SerialName("grid") val grid: List<List<String>> = emptyList(),
) {
    companion object {
        /**
         * Фиксированная размерность сетки. Известна обеим сторонам: сервер рисует такую
         * сетку поверх картинки ([ScoreboardColorExtractor.drawGridOverlay]), а модель
         * возвращает матрицу ролей того же размера. Стартовое значение калибруется на
         * golden-сете реальных табло.
         */
        const val GRID_ROWS: Int = 8
        const val GRID_COLS: Int = 20
    }
}
