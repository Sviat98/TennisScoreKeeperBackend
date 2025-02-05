package com.bashkevich.tennisscorekeeperbackend.plugins

import com.bashkevich.tennisscorekeeperbackend.feature.counter.counterRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        counterRoutes()
    }
}
