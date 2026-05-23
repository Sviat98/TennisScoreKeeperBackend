package com.bashkevich.tennisscorekeeperbackend.feature.player

import com.bashkevich.tennisscorekeeperbackend.model.auth.JWT_AUTH
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerBodyDto
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerDto
import com.bashkevich.tennisscorekeeperbackend.plugins.receiveBodyCatching
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.openapi.Headers
import io.ktor.openapi.ParameterType
import io.ktor.openapi.jsonSchema
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.header
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.ktor.ext.inject

@OptIn(ExperimentalKtorApi::class)
fun Route.playerRoutes() {
    val playerService by application.inject<PlayerService>()

    route("/players") {
        authenticate(JWT_AUTH) {
            /**
             * Tag: Player
             * Create a new player.
             */
            post {
                val playerBodyDto = call.receiveBodyCatching<PlayerBodyDto>()

                val newPlayer = playerService.addPlayer(playerBodyDto = playerBodyDto)

                call.respond(HttpStatusCode.Created, newPlayer)
            }.describe {
                requestBody {
                    description = "Player data to create"
                    schema = jsonSchema<PlayerBodyDto>()
                }
                responses {
                    HttpStatusCode.Created {
                        description = "Player created successfully"
                        schema = jsonSchema<PlayerDto>()
                    }
                    HttpStatusCode.BadRequest {
                        description = "Invalid request body or validation error"
                        ContentType.Text.Plain()
                    }
                    HttpStatusCode.Unauthorized {
                        description = "Missing or invalid JWT token"
                        ContentType.Text.Plain()
                    }
                }
            }
        }
    }
}
