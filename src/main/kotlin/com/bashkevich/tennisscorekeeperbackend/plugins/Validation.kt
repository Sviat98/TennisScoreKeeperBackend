package com.bashkevich.tennisscorekeeperbackend.plugins

import com.bashkevich.tennisscorekeeperbackend.feature.player.PlayerService
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult
import io.ktor.server.request.ContentTransformationException
import org.koin.ktor.ext.inject

fun Application.configureValidation(){
    val playerService by inject<PlayerService>()

    install(RequestValidation) {
            validate<MatchBody> { body ->
                val firstPlayerId = body.firstPlayerId.toIntOrNull() ?: 0

                val secondPlayerId = body.secondPlayerId.toIntOrNull() ?: 0

                val firstPlayer = playerService.getPlayerById(firstPlayerId)

                val secondPlayer = playerService.getPlayerById(secondPlayerId)

                if (firstPlayerId == secondPlayerId)
                    ValidationResult.Invalid("Players should be different!")
                else ValidationResult.Valid
            }
    }
}


suspend inline fun <reified T : Any> validateBody(
    requestBody: T,
    noinline validation: suspend (T) -> Boolean,
){
    try {
        if (!validation(requestBody)) {
            throw BadRequestException("Invalid request body format 111")
        }
    } catch (e: ContentTransformationException) {
        throw BadRequestException("Invalid request body format")
    } catch (e: Exception) {
        if (e !is BadRequestException){
            throw InternalServerErrorException("Invalid request body format")
        }
    }
}