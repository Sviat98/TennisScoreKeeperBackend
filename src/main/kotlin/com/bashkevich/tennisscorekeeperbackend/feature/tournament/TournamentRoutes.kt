package com.bashkevich.tennisscorekeeperbackend.feature.tournament

import com.bashkevich.tennisscorekeeperbackend.model.auth.JWT_AUTH
import com.bashkevich.tennisscorekeeperbackend.model.message.ResponseMessageDto
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentDto
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentRequestDto
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentStatusBody
import com.bashkevich.tennisscorekeeperbackend.plugins.receiveBodyCatching
import com.bashkevich.tennisscorekeeperbackend.plugins.respondWithMessageBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.ktor.ext.inject

@OptIn(ExperimentalKtorApi::class)
fun Route.tournamentRoutes() {
    val tournamentService by application.inject<TournamentService>()

    route("/tournaments") {
        /**
         * Tag: Tournament
         * Get all tournaments.
         */
        get {
            val tournaments = tournamentService.getTournaments()

            call.respond(tournaments)
        }.describe {
            responses {
                HttpStatusCode.OK {
                    description = "Successfully retrieved all tournaments"
                    schema = jsonSchema<List<TournamentDto>>()
                }
            }
        }
        authenticate(JWT_AUTH) {
            /**
             * Tag: Tournament
             * Create a new tournament.
             */
            post {
                val tournamentRequestDto = call.receiveBodyCatching<TournamentRequestDto>()

                val newTournament = tournamentService.addTournament(tournamentRequestDto = tournamentRequestDto)

                call.respond(status = HttpStatusCode.Created, message = newTournament)
            }.describe {
                requestBody {
                    description = "Tournament data to create"
                    schema = jsonSchema<TournamentRequestDto>()
                }
                responses {
                    HttpStatusCode.Created {
                        description = "Tournament created successfully"
                        schema = jsonSchema<TournamentDto>()
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
        route("/{id}") {
            /**
             * Tag: Tournament
             * Get tournament by ID.
             */
            get {
                val tournamentId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                val tournament = tournamentService.getTournamentById(tournamentId)

                call.respond(tournament)
            }.describe {
                responses {
                    HttpStatusCode.OK {
                        description = "Successfully retrieved tournament"
                        schema = jsonSchema<TournamentDto>()
                    }
                    HttpStatusCode.BadRequest {
                        description = "Invalid tournament ID"
                        ContentType.Text.Plain()
                    }
                    HttpStatusCode.NotFound {
                        description = "Tournament not found"
                        ContentType.Text.Plain()
                    }
                }
            }
            authenticate(JWT_AUTH) {
                /**
                 * Tag: Tournament
                 * Update tournament status.
                 */
                patch("/status") {
                    val tournamentId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                    val tournamentStatusBody = call.receiveBodyCatching<TournamentStatusBody>()

                    tournamentService.updateTournamentStatus(tournamentId, tournamentStatusBody)

                    call.respondWithMessageBody(message = "Successfully updated status to ${tournamentStatusBody.status}")
                }.describe {
                    requestBody {
                        description = "New tournament status"
                        schema = jsonSchema<TournamentStatusBody>()
                    }
                    responses {
                        HttpStatusCode.OK {
                            description = "Tournament status updated successfully"
                            schema = jsonSchema<ResponseMessageDto>()
                        }
                        HttpStatusCode.BadRequest {
                            description = "Invalid request body or tournament ID"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.Unauthorized {
                            description = "Missing or invalid JWT token"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.NotFound {
                            description = "Tournament not found"
                            ContentType.Text.Plain()
                        }
                    }
                }
            }
        }
    }
}
