package com.bashkevich.tennisscorekeeperbackend.model.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Результат текстового описания теннисного табло по изображению
 * (эндпоинт /themes/ai/describeMatch).
 *
 * Разделён на два варианта (on_success / on_failure), чтобы AI мог явно сообщить,
 * что картинка не является теннисным табло. Семантика полей описана в system-промпте
 * ThemeService.DESCRIBE_MATCH_PROMPT.
 */
@Serializable
sealed class MatchDescriptionResult {

    @Serializable
    @SerialName("on_success")
    data class OnSuccess(
        val description: String,
    ) : MatchDescriptionResult()

    @Serializable
    @SerialName("on_failure")
    data class OnFailure(
        val reason: String,
    ) : MatchDescriptionResult()
}
