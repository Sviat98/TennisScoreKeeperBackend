package com.bashkevich.tennisscorekeeperbackend.feature.participant

import com.bashkevich.tennisscorekeeperbackend.model.auth.JWT_AUTH
import com.bashkevich.tennisscorekeeperbackend.model.participant.ParticipantDto
import com.bashkevich.tennisscorekeeperbackend.plugins.receiveMultipartCatching
import io.ktor.http.ContentType
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
                    ContentType.Text.Plain()
                }
                HttpStatusCode.NotFound {
                    description = "Tournament not found"
                    ContentType.Text.Plain()
                }
            }
        }
        post {

        }
        authenticate(JWT_AUTH) {
            /**
             * Tag: Participant
             * Upload participants from an Excel file.
             */
            post("/upload") {
                val tournamentId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                val file = call.receiveMultipartCatching()

                val participants = participantServiceRouter.uploadParticipants(tournamentId, file)

                call.respond(HttpStatusCode.Created, participants)
            }.describe {
                requestBody {
                    description = "Excel file with participant data (.xlsx)"
                }
                responses {
                    HttpStatusCode.Created {
                        description = "Participants uploaded successfully"
                        schema = jsonSchema<List<ParticipantDto>>()
                    }
                    HttpStatusCode.BadRequest {
                        description = "Invalid tournament ID, missing file, or wrong file type"
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
