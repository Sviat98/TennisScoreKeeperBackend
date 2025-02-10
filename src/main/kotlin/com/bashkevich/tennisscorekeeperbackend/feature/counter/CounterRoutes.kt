package com.bashkevich.tennisscorekeeperbackend.feature.counter

import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterBodyDto
import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterDeltaDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
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

                val counter = counterService.changeCounterValue(counterId = id, counterDelta = bodyWithDelta.delta)

                call.respond(counter)
            }
        }


    }


}