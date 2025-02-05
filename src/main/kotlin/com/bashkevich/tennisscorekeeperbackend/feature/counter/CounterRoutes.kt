package com.bashkevich.tennisscorekeeperbackend.feature.counter

import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterBodyDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject


fun Route.counterRoutes(){
    val counterService by application.inject<CounterService>()

    get("/counters") {
        val counters = counterService.getCounters()
        call.respond(counters)
    }
    get("/counters/{id}") {
        val id = call.parameters["id"]?.toIntOrNull() ?: 0

        val counter = counterService.getCounterById(counterId = id)

        call.respond(counter)
    }
    post("/counter") {
       val counterBody =  call.receive(CounterBodyDto::class)

        val newCounter = counterService.addCounter(counterName = counterBody.name)

        call.respond(HttpStatusCode.Created,newCounter)
    }
}