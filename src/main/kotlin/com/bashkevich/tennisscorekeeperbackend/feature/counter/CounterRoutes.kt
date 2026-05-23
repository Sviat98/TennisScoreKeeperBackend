package com.bashkevich.tennisscorekeeperbackend.feature.counter

import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterBodyDto
import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterConnectionManager
import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterDeltaDto
import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterDto
import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterObserver
import com.bashkevich.tennisscorekeeperbackend.model.counter.toDto
import com.bashkevich.tennisscorekeeperbackend.model.message.ResponseMessageDto
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.ExperimentalKtorApi
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

@OptIn(ExperimentalKtorApi::class)
fun Route.counterRoutes() {
    val counterService by application.inject<CounterService>()
    route("/counters") {
        /**
         * Tag: Counter
         * Get all counters.
         */
        get {
            val counters = counterService.getCounters()
            call.respond(counters)
        }.describe {
            responses {
                HttpStatusCode.OK {
                    description = "Successfully retrieved all counters"
                    schema = jsonSchema<List<CounterDto>>()
                }
            }
        }
        /**
         * Tag: Counter
         * Create a new counter.
         */
        post {
            val counterBody = call.receive(CounterBodyDto::class)

            val newCounter = counterService.addCounter(counterName = counterBody.name, counterValue = counterBody.value)

            call.respond(HttpStatusCode.Created, newCounter)
        }.describe {
            requestBody {
                description = "Counter data to create"
                schema = jsonSchema<CounterBodyDto>()
            }
            responses {
                HttpStatusCode.Created {
                    description = "Counter created successfully"
                    schema = jsonSchema<CounterDto>()
                }
                HttpStatusCode.BadRequest {
                    description = "Invalid request body"
                    ContentType.Text.Plain()
                }
            }
        }
        route("/{id}") {
            /**
             * Tag: Counter
             * Get counter by ID.
             */
            get {
                val id = call.parameters["id"]?.toIntOrNull() ?: 0

                val counter = counterService.getCounterById(counterId = id)

                call.respond(counter)
            }.describe {
                responses {
                    HttpStatusCode.OK {
                        description = "Successfully retrieved counter"
                        schema = jsonSchema<CounterDto>()
                    }
                    HttpStatusCode.BadRequest {
                        description = "Invalid counter ID"
                        ContentType.Text.Plain()
                    }
                    HttpStatusCode.NotFound {
                        description = "Counter not found"
                        ContentType.Text.Plain()
                    }
                }
            }
            /**
             * Tag: Counter
             * Update counter value by a delta.
             */
            patch {
                val id = call.parameters["id"]?.toIntOrNull() ?: 0

                val bodyWithDelta = call.receive(CounterDeltaDto::class)

                counterService.changeCounterValue(counterId = id, counterDelta = bodyWithDelta.delta)

                call.respond(HttpStatusCode.OK, "Counter successfully changed")
            }.describe {
                requestBody {
                    description = "Delta to apply to counter value"
                    schema = jsonSchema<CounterDeltaDto>()
                }
                responses {
                    HttpStatusCode.OK {
                        description = "Counter value updated successfully"
                        schema = jsonSchema<ResponseMessageDto>()
                    }
                    HttpStatusCode.BadRequest {
                        description = "Invalid request body or counter ID"
                        ContentType.Text.Plain()
                    }
                    HttpStatusCode.NotFound {
                        description = "Counter not found"
                        ContentType.Text.Plain()
                    }
                }
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
