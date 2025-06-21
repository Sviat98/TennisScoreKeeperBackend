package com.bashkevich.tennisscorekeeperbackend.plugins

import com.bashkevich.tennisscorekeeperbackend.model.match.body.ChangeScoreBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ScoreType
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult

fun Application.configureValidation() {

    install(RequestValidation) {
        validate<MatchBody> { body ->
            val firstPlayerId = body.firstParticipant.id.toIntOrNull() ?: 0

            val secondPlayerId = body.secondParticipant.id.toIntOrNull() ?: 0

            val setsToWin = body.setsToWin

            val regularSetId = body.regularSet?.toIntOrNull() ?: 0
            val decidingSetId = body.decidingSet.toIntOrNull() ?: 0

            when {
                firstPlayerId == 0 -> ValidationResult.Invalid("First player id is wrong!")
                secondPlayerId == 0 -> ValidationResult.Invalid("Second player id is wrong!")
                firstPlayerId == secondPlayerId -> ValidationResult.Invalid("Players should be different!")
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
    }
}

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