package com.bashkevich.tennisscorekeeperbackend.model.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Результат анализа изображения на наличие теннисного табло.
 *
 * Намеренно плоская структура (а не sealed-иерархия on_success/on_failure):
 * корневой тип должен быть обычным классом. При полиморфном (sealed) корне
 * OpenAI-генератор JSON-схемы в Koog (OpenAIStandardJsonSchemaGenerator) безусловно
 * ждёт на корне "$ref", которого для sealed-корня нет (там oneOf), и падает с
 * `NoSuchElementException: Key $ref is missing in the map`. Поле [isScoreboard]
 * даёт модели легальный способ отказаться от обработки в рамках structured output.
 *
 * Семантика полей описана в system-промпте ThemeService.SYSTEM_PROMPT,
 * поэтому сама модель остаётся без Koog-зависимостей.
 */
@Serializable
data class AiThemeExtractionResult(
    @SerialName("is_scoreboard") val isScoreboard: Boolean,
    @SerialName("reason") val reason: String? = null,
    @SerialName("theme") val theme: ThemeContent? = null,
)
