package com.bashkevich.tennisscorekeeperbackend.plugins

import com.bashkevich.tennisscorekeeperbackend.model.match.body.ChangeScoreBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.ParticipantInMatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.body.RetiredParticipantBody
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ScoreType
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ServeBody
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ServeInPairBody
import com.bashkevich.tennisscorekeeperbackend.model.match.body.UpdateMatchBody
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult

fun Application.configureValidation() {

    install(RequestValidation) {
        validate<MatchBody> { body ->
            val firstParticipantError = participantInMatchError("First player", body.firstParticipant)

            val secondParticipantError = participantInMatchError("Second player", body.secondParticipant)

            val setsToWin = body.setsToWin

            val regularSetId = body.regularSet?.toIntOrNull() ?: 0
            val decidingSetId = body.decidingSet.toIntOrNull() ?: 0

            when {
                firstParticipantError != null -> ValidationResult.Invalid(firstParticipantError)
                secondParticipantError != null -> ValidationResult.Invalid(secondParticipantError)
                body.firstParticipant.id.toInt() == body.secondParticipant.id.toInt() -> ValidationResult.Invalid("Players should be different!")
                regularSetId == 0 && setsToWin > 1 -> ValidationResult.Invalid("Regular set id is wrong or empty!")
                regularSetId != 0 && setsToWin < 2 -> ValidationResult.Invalid("Regular set is redundant!")
                decidingSetId == 0 -> ValidationResult.Invalid("Deciding set id is wrong!")
                else -> ValidationResult.Valid
            }
        }
        validate<ChangeScoreBody> { body ->

            val scoringPlayerId = body.participantId.toIntOrNull() ?: 0

            when {
                scoringPlayerId == 0 -> ValidationResult.Invalid("Scoring player id is wrong!")
                body.scoreType !in listOf(
                    ScoreType.GAME,
                    ScoreType.POINT
                ) -> ValidationResult.Invalid("Wrong score type!")

                else -> ValidationResult.Valid
            }
        }
        validate<ServeBody> { body ->

            val servingParticipantId = body.servingParticipantId.toIntOrNull() ?: 0

            when {
                servingParticipantId == 0 -> ValidationResult.Invalid("Serving participant id is wrong!")
                else -> ValidationResult.Valid
            }
        }
        validate<ServeInPairBody> { body ->
            val servingPlayerId = body.servingPlayerId.toIntOrNull() ?: 0

            when {
                servingPlayerId == 0 -> ValidationResult.Invalid("Serving player id in pair is wrong!")
                else -> ValidationResult.Valid
            }
        }
        validate<RetiredParticipantBody> { body ->

            val retiredParticipantId = body.retiredParticipantId.toIntOrNull() ?: 0

            when {
                retiredParticipantId == 0 -> ValidationResult.Invalid("Retired participant id is wrong!")
                else -> ValidationResult.Valid
            }
        }
        validate<UpdateMatchBody> { body ->

            val firstParticipantError = participantInMatchError("First participant", body.firstParticipant)

            val secondParticipantError = participantInMatchError("Second participant", body.secondParticipant)

            val themeId = body.themeId.toIntOrNull() ?: 0

            when {
                firstParticipantError != null -> ValidationResult.Invalid(firstParticipantError)
                secondParticipantError != null -> ValidationResult.Invalid(secondParticipantError)
                body.firstParticipant.displayName.isBlank() -> ValidationResult.Invalid("First participant display name is empty!")
                body.secondParticipant.displayName.isBlank() -> ValidationResult.Invalid("Second participant display name is empty!")
                themeId == 0 -> ValidationResult.Invalid("Theme id is wrong!")
                else -> ValidationResult.Valid
            }
        }
    }
}

// id и hex-формат цветов участника проверяются и при создании матча, и при его редактировании
private fun participantInMatchError(label: String, participant: ParticipantInMatchBody): String? {
    val id = participant.id.toIntOrNull() ?: 0

    return when {
        id == 0 -> "$label id is wrong!"
        !participant.primaryColor.matches(HEX_COLOR_REGEX) -> "$label primary color is wrong!"
        participant.secondaryColor?.matches(HEX_COLOR_REGEX) == false -> "$label secondary color is wrong!"
        else -> null
    }
}

private val HEX_COLOR_REGEX = Regex("^[0-9A-Fa-f]{6}$")

// данная валидация нужна для проверки сущностей в базе
suspend inline fun validateRequestConditions(
    noinline validation: suspend () -> String,
) {
    val errorMessage = validation()
    println(errorMessage)
    if (errorMessage.isNotEmpty()) {
        throw BadRequestException(errorMessage)
    }
}