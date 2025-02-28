package com.bashkevich.tennisscorekeeperbackend.feature.counter

import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterBodyDto
import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterConnectionManager
import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterDeltaDto
import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterObserver
import com.bashkevich.tennisscorekeeperbackend.model.counter.toDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject

fun Route.counterRoutes() {
    val counterService by application.inject<CounterService>()
    route("/counters") {
        get {
            val counters = counterService.getCounters()
            call.respond(counters)
        }
        post {
            val counterBody = call.receive(CounterBodyDto::class)

            val newCounter = counterService.addCounter(counterName = counterBody.name, counterValue = counterBody.value)

            call.respond(HttpStatusCode.Created, newCounter)
        }
        route("/{id}") {
            get {
                val id = call.parameters["id"]?.toIntOrNull() ?: 0

                val counter = counterService.getCounterById(counterId = id)

                call.respond(counter)
            }
            patch {
                val id = call.parameters["id"]?.toIntOrNull() ?: 0

                val bodyWithDelta = call.receive(CounterDeltaDto::class)

                counterService.changeCounterValue(counterId = id, counterDelta = bodyWithDelta.delta)

                call.respond(HttpStatusCode.OK, "Counter successfully changed")
            }
            webSocket {
                val id = call.parameters["id"]?.toIntOrNull() ?: 0

                if (id == 0) {
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid counter ID"))
                    return@webSocket
                }

                CounterConnectionManager.addConnection(id, this)
                val isUpdater = CounterConnectionManager.getFirstConnection(id) == this

                val counterFlow = CounterObserver.getCounterFlow(id)

                println("replay cache: ${counterFlow.replayCache}")

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
                    counterFlow.map { counterEntity -> counterEntity.toDto() }.onStart {
                        if(counterFlow.replayCache.isEmpty()){
                            val initialCounter = counterService.getCounterById(id)
                            emit(initialCounter)
                        }
                    }.collectLatest { counterDto ->
                        sendSerialized(counterDto) // Send JSON response
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
                    CounterConnectionManager.removeConnection(id, this)
                    if (isActive) {
                        close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server is closing the connection"))
                    }
                }

            }
        }
    }

}