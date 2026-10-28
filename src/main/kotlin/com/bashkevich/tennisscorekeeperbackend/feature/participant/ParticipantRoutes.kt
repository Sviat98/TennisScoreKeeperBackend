package com.bashkevich.tennisscorekeeperbackend.feature.participant

import com.bashkevich.tennisscorekeeperbackend.model.auth.JWT_AUTH
import com.bashkevich.tennisscorekeeperbackend.model.message.ResponseMessageDto
import com.bashkevich.tennisscorekeeperbackend.model.participant.ParticipantDto
import com.bashkevich.tennisscorekeeperbackend.model.participant.ParticipantDownloadRequestDto
import com.bashkevich.tennisscorekeeperbackend.plugins.receiveBodyCatching
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.ktor.ext.inject

@OptIn(ExperimentalKtorApi::class)
fun Route.participantRoutes() {

    val participantServiceRouter by application.inject<ParticipantServiceRouter>()

    route("/tournaments/{id}/participants") {
        /**
         * Tag: Participant
         * Get all participants of a tournament.
         */
        get {
            val tournamentId = call.pathParameters["id"]?.toIntOrNull() ?: 0

            val participants = participantServiceRouter.getParticipantsByTournament(tournamentId)

            call.respond(participants)
        }.describe {
            responses {
                HttpStatusCode.OK {
                    description = "Successfully retrieved participant list"
                    schema = jsonSchema<List<ParticipantDto>>()
                }
                HttpStatusCode.BadRequest {
                    description = "Invalid tournament ID"
                    schema = jsonSchema<ResponseMessageDto>()
                }
                HttpStatusCode.NotFound {
                    description = "Tournament not found"
                    schema = jsonSchema<ResponseMessageDto>()
                }
            }
        }
        post {

        }
        authenticate(JWT_AUTH) {
            /**
             * Tag: Participant
             * Download participants from the League of Tennis API registration list.
             */
            post("/download") {
                val tournamentId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                val downloadRequest = call.receiveBodyCatching<ParticipantDownloadRequestDto>()

                val participants = participantServiceRouter.downloadParticipants(tournamentId, downloadRequest)

                call.respond(HttpStatusCode.Created, participants)
            }.describe {
                requestBody {
                    description = "Optional seed amount for participants"
                    schema = jsonSchema<ParticipantDownloadRequestDto>()
                }
                responses {
                    HttpStatusCode.Created {
                        description = "Participants downloaded successfully"
                        schema = jsonSchema<List<ParticipantDto>>()
                    }
                    HttpStatusCode.BadRequest {
                        description = "Invalid tournament ID, tournament is completed or is not linked to the League of Tennis API, or no players registered for the tournament in the League of Tennis API"
                        schema = jsonSchema<ResponseMessageDto>()
                    }
                    HttpStatusCode.Unauthorized {
                        description = "Missing or invalid JWT token"
                        schema = jsonSchema<ResponseMessageDto>()
                    }
                    HttpStatusCode.NotFound {
                        description = "Tournament not found"
                        schema = jsonSchema<ResponseMessageDto>()
                    }
                }
            }
        }
    }
}
