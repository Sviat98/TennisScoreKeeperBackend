package com.bashkevich.tennisscorekeeperbackend.model.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Результат извлечения темы табло из изображения.
 *
 * Разделён на два варианта (on_success / on_failure), чтобы AI мог явно сообщить,
 * что картинка не является теннисным табло и не подлежит обработке.
 *
 * Семантика полей описана в system-промпте ThemeService.SYSTEM_PROMPT,
 * поэтому сама модель ThemeContent остаётся без Koog-зависимостей.
 */
@Serializable
sealed class AiThemeExtractionResult {

    @Serializable
    @SerialName("on_success")
    data class OnSuccess(
        val theme: ThemeContent,
    ) : AiThemeExtractionResult()

    @Serializable
    @SerialName("on_failure")
    data class OnFailure(
        val reason: String,
    ) : AiThemeExtractionResult()
}
