package com.bashkevich.tennisscorekeeperbackend.feature.match

import com.bashkevich.tennisscorekeeperbackend.feature.match.websocket.MatchConnectionManager
import com.bashkevich.tennisscorekeeperbackend.feature.match.websocket.MatchObserver
import com.bashkevich.tennisscorekeeperbackend.model.match.ChangeScoreBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.ServeBody
import com.bashkevich.tennisscorekeeperbackend.plugins.receiveBodyCatching
import com.bashkevich.tennisscorekeeperbackend.plugins.respondWithMessageBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject

fun Route.matchRoutes(){
    val matchServiceRouter by application.inject<MatchServiceRouter>()

    route("/tournaments/{id}/matches"){
        get {
            val tournamentId = call.pathParameters["id"]?.toIntOrNull() ?: 0

            val matchList = matchServiceRouter.getMatchesByTournament(tournamentId)

            call.respond(matchList)
        }
        post {
            val tournamentId = call.pathParameters["id"]?.toIntOrNull() ?: 0

            val matchBody = call.receiveBodyCatching<MatchBody>()

            val newMatch = matchServiceRouter.addMatch(tournamentId,matchBody)

            call.respond(HttpStatusCode.Created,newMatch)
        }
    }

    route("/matches") {
        route("/{id}") {
            get {
                val id = call.pathParameters["id"]?.toIntOrNull() ?: 0

                val matchDto = matchServiceRouter.getMatchById(id)

                call.respond(matchDto)
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

                // Only fetch from DB if there are no existing updates in counterFlow
//                val hasUpdates = counterFlow.replayCache.isNotEmpty()
//
//                println("hasUpdates = $hasUpdates")
//
//                if (!hasUpdates) {
//                    val initialCounter = counterService.getCounterById(id)
//
//                    sendSerialized(initialCounter)
//                }

                val job = launch {
                    matchFlow.onStart {
                        if(matchFlow.replayCache.isEmpty()){
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
            patch("/serve"){
                val matchId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                val serveBody = call.receiveBodyCatching<ServeBody>()

                matchServiceRouter.updateServe(matchId,serveBody)

                call.respondWithMessageBody(message ="Successfully chose serve")
            }
            patch("/score"){
                val matchId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                val changeScoreBody = call.receiveBodyCatching<ChangeScoreBody>()

                matchServiceRouter.updateScore(matchId,changeScoreBody)

                call.respondWithMessageBody(message ="Successfully updated the score")
            }
            patch("/undo"){
                val matchId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                matchServiceRouter.undoPoint(matchId)

                call.respondWithMessageBody(message = "Successfully undone the point")
            }
            patch("/redo"){
                val matchId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                matchServiceRouter.redoPoint(matchId)

                call.respondWithMessageBody(message ="Successfully redone the point")
            }
        }
   }
}