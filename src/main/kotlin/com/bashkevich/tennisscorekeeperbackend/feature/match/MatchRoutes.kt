package com.bashkevich.tennisscorekeeperbackend.feature.match

import com.bashkevich.tennisscorekeeperbackend.feature.match.websocket.MatchConnectionManager
import com.bashkevich.tennisscorekeeperbackend.feature.match.websocket.MatchObserver
import com.bashkevich.tennisscorekeeperbackend.model.auth.JWT_AUTH
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.ShortMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ChangeScoreBody
import com.bashkevich.tennisscorekeeperbackend.model.match.body.MatchStatusBody
import com.bashkevich.tennisscorekeeperbackend.model.match.body.RetiredParticipantBody
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ServeBody
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ServeInPairBody
import com.bashkevich.tennisscorekeeperbackend.model.match.body.VideoLinkBody
import com.bashkevich.tennisscorekeeperbackend.model.message.ResponseMessageDto
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
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.ExperimentalKtorApi
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject

@OptIn(ExperimentalKtorApi::class)
fun Route.matchRoutes() {
    val matchServiceRouter by application.inject<MatchServiceRouter>()

    route("/tournaments/{id}/matches") {
        /**
         * Tag: Match
         * Get all matches for a tournament.
         */
        get {
            val tournamentId = call.pathParameters["id"]?.toIntOrNull() ?: 0

            val matchList = matchServiceRouter.getMatchesByTournament(tournamentId)

            call.respond(matchList)
        }.describe {
            responses {
                HttpStatusCode.OK {
                    description = "Successfully retrieved match list"
                    schema = jsonSchema<List<ShortMatchDto>>()
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
             * Tag: Match
             * Create a new match in a tournament.
             */
            post {
                val tournamentId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                val matchBody = call.receiveBodyCatching<MatchBody>()

                val newMatch = matchServiceRouter.addMatch(tournamentId, matchBody)

                call.respond(HttpStatusCode.Created, newMatch)
            }.describe {
                requestBody {
                    description = "Match data to create"
                    schema = jsonSchema<MatchBody>()
                }
                responses {
                    HttpStatusCode.Created {
                        description = "Match created successfully"
                        schema = jsonSchema<ShortMatchDto>()
                    }
                    HttpStatusCode.BadRequest {
                        description = "Invalid request body or tournament not in progress"
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

    route("/matches") {
        route("/{id}") {
            /**
             * Tag: Match
             * Get full match details by ID.
             */
            get {
                val id = call.pathParameters["id"]?.toIntOrNull() ?: 0

                val matchDto = matchServiceRouter.getMatchById(id)

                call.respond(matchDto)
            }.describe {
                responses {
                    HttpStatusCode.OK {
                        description = "Successfully retrieved match details"
                        schema = jsonSchema<MatchDto>()
                    }
                    HttpStatusCode.BadRequest {
                        description = "Invalid match ID"
                        ContentType.Text.Plain()
                    }
                    HttpStatusCode.NotFound {
                        description = "Match not found"
                        ContentType.Text.Plain()
                    }
                }
            }
            webSocket {
                val id = call.parameters["id"]?.toIntOrNull() ?: 0

                if (id == 0) {
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid match ID"))
                    return@webSocket
                }

                MatchConnectionManager.addConnection(id, this)
                val isUpdater = MatchConnectionManager.getFirstConnection(id) == this

                val matchFlow = MatchObserver.getMatchFlow(id)

                println("replay cache: ${matchFlow.replayCache}")

                val job = launch {
                    matchFlow.onStart {
                        if (matchFlow.replayCache.isEmpty()) {
                            val initialMatch = matchServiceRouter.getMatchById(id)
                            emit(initialMatch)
                        }
                    }.collectLatest { matchDto ->
                        sendSerialized(matchDto) // Send JSON response
                    }
                }

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Close) {
                            close(CloseReason(CloseReason.Codes.NORMAL, "Client closed connection"))
                            break
                        }
                    }
                } finally {
                    job.cancel() // Cancel the coroutine when WebSocket is closed
                    MatchConnectionManager.removeConnection(id, this)
                    if (isActive) {
                        close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server is closing the connection"))
                    }
                }
            }
            authenticate(JWT_AUTH) {
                /**
                 * Tag: Match
                 * Set which participant serves first.
                 */
                patch("/firstServe") {
                    val matchId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                    val serveBody = call.receiveBodyCatching<ServeBody>()

                    matchServiceRouter.updateServe(matchId, serveBody)

                    call.respondWithMessageBody(message = "Successfully chose first serve")
                }.describe {
                    requestBody {
                        description = "Participant ID who serves first"
                        schema = jsonSchema<ServeBody>()
                    }
                    responses {
                        HttpStatusCode.OK {
                            description = "First serve set successfully"
                            schema = jsonSchema<ResponseMessageDto>()
                        }
                        HttpStatusCode.BadRequest {
                            description = "Invalid request body or match ID"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.Unauthorized {
                            description = "Missing or invalid JWT token"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.NotFound {
                            description = "Match not found"
                            ContentType.Text.Plain()
                        }
                    }
                }
                /**
                 * Tag: Match
                 * Set which player serves first in a pair (doubles only).
                 */
                patch("/firstServeInPair") {
                    val matchId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                    val serveInPairBody = call.receiveBodyCatching<ServeInPairBody>()

                    matchServiceRouter.updateServeInPair(matchId, serveInPairBody)

                    call.respondWithMessageBody(message = "Successfully chose first serve in pair")
                }.describe {
                    requestBody {
                        description = "Player ID who serves first in the pair"
                        schema = jsonSchema<ServeInPairBody>()
                    }
                    responses {
                        HttpStatusCode.OK {
                            description = "First serve in pair set successfully"
                            schema = jsonSchema<ResponseMessageDto>()
                        }
                        HttpStatusCode.BadRequest {
                            description = "Invalid request body, match ID, or not a doubles match"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.Unauthorized {
                            description = "Missing or invalid JWT token"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.NotFound {
                            description = "Match not found"
                            ContentType.Text.Plain()
                        }
                    }
                }
                /**
                 * Tag: Match
                 * Update match status (start, pause, complete).
                 */
                patch("/status") {
                    val matchId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                    val matchStatusBody = call.receiveBodyCatching<MatchStatusBody>()

                    matchServiceRouter.updateMatchStatus(matchId, matchStatusBody)

                    call.respondWithMessageBody(message = "Successfully updated status to ${matchStatusBody.status}")
                }.describe {
                    requestBody {
                        description = "New match status"
                        schema = jsonSchema<MatchStatusBody>()
                    }
                    responses {
                        HttpStatusCode.OK {
                            description = "Match status updated successfully"
                            schema = jsonSchema<ResponseMessageDto>()
                        }
                        HttpStatusCode.BadRequest {
                            description = "Invalid request body or status transition"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.Unauthorized {
                            description = "Missing or invalid JWT token"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.NotFound {
                            description = "Match not found"
                            ContentType.Text.Plain()
                        }
                    }
                }
                /**
                 * Tag: Match
                 * Update match score. Supports point, game, set scoring and retirements.
                 */
                patch("/score") {
                    val matchId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                    val changeScoreBody = call.receiveBodyCatching<ChangeScoreBody>()

                    matchServiceRouter.updateScore(matchId, changeScoreBody)

                    call.respondWithMessageBody(message = "Successfully updated the score")
                }.describe {
                    requestBody {
                        description = "Score change: participant ID and score type (POINT, TIEBREAK_POINT, GAME, SET, etc.)"
                        schema = jsonSchema<ChangeScoreBody>()
                    }
                    responses {
                        HttpStatusCode.OK {
                            description = "Score updated successfully"
                            schema = jsonSchema<ResponseMessageDto>()
                        }
                        HttpStatusCode.BadRequest {
                            description = "Invalid request body or score change"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.Unauthorized {
                            description = "Missing or invalid JWT token"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.NotFound {
                            description = "Match not found"
                            ContentType.Text.Plain()
                        }
                    }
                }
                /**
                 * Tag: Match
                 * Retire a participant from the match.
                 */
                patch("/retire") {
                    val matchId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                    val retiredParticipantBody = call.receiveBodyCatching<RetiredParticipantBody>()

                    matchServiceRouter.setParticipantRetired(matchId, retiredParticipantBody)

                    call.respondWithMessageBody(message = "Participant successfully retired")
                }.describe {
                    requestBody {
                        description = "ID of the retiring participant"
                        schema = jsonSchema<RetiredParticipantBody>()
                    }
                    responses {
                        HttpStatusCode.OK {
                            description = "Participant retired successfully"
                            schema = jsonSchema<ResponseMessageDto>()
                        }
                        HttpStatusCode.BadRequest {
                            description = "Invalid request body or match ID"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.Unauthorized {
                            description = "Missing or invalid JWT token"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.NotFound {
                            description = "Match or participant not found"
                            ContentType.Text.Plain()
                        }
                    }
                }
                /**
                 * Tag: Match
                 * Undo the last scored point.
                 */
                patch("/undo") {
                    val matchId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                    matchServiceRouter.undoPoint(matchId)

                    call.respondWithMessageBody(message = "Successfully undone the point")
                }.describe {
                    responses {
                        HttpStatusCode.OK {
                            description = "Point undone successfully"
                            schema = jsonSchema<ResponseMessageDto>()
                        }
                        HttpStatusCode.BadRequest {
                            description = "Nothing to undo"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.Unauthorized {
                            description = "Missing or invalid JWT token"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.NotFound {
                            description = "Match not found"
                            ContentType.Text.Plain()
                        }
                    }
                }
                /**
                 * Tag: Match
                 * Redo the last undone point.
                 */
                patch("/redo") {
                    val matchId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                    matchServiceRouter.redoPoint(matchId)

                    call.respondWithMessageBody(message = "Successfully redone the point")
                }.describe {
                    responses {
                        HttpStatusCode.OK {
                            description = "Point redone successfully"
                            schema = jsonSchema<ResponseMessageDto>()
                        }
                        HttpStatusCode.BadRequest {
                            description = "Nothing to redo"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.Unauthorized {
                            description = "Missing or invalid JWT token"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.NotFound {
                            description = "Match not found"
                            ContentType.Text.Plain()
                        }
                    }
                }
                /**
                 * Tag: Match
                 * Set or update video link for the match.
                 */
                patch("/video") {
                    val matchId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                    val videoLinkBody = call.receiveBodyCatching<VideoLinkBody>()

                    val videoLink = videoLinkBody.videoLink

                    matchServiceRouter.setVideoLink(matchId, videoLink)

                    call.respondWithMessageBody(message = "Successfully added video link")
                }.describe {
                    requestBody {
                        description = "Video link to attach to the match"
                        schema = jsonSchema<VideoLinkBody>()
                    }
                    responses {
                        HttpStatusCode.OK {
                            description = "Video link added successfully"
                            schema = jsonSchema<ResponseMessageDto>()
                        }
                        HttpStatusCode.BadRequest {
                            description = "Invalid request body or match ID"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.Unauthorized {
                            description = "Missing or invalid JWT token"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.NotFound {
                            description = "Match not found"
                            ContentType.Text.Plain()
                        }
                    }
                }
            }

        }
    }
}
