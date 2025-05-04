package com.bashkevich.tennisscorekeeperbackend.plugins

import com.bashkevich.tennisscorekeeperbackend.feature.counter.counterRoutes
import com.bashkevich.tennisscorekeeperbackend.feature.match.matchRoutes
import com.bashkevich.tennisscorekeeperbackend.feature.player.playerRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        get("/dbHealth") {
            // проверка доступности БД
            if (isDbConnected()) call.respond("OK")
            else call.respond(HttpStatusCode.ServiceUnavailable)
        }
        counterRoutes()
        playerRoutes()
        matchRoutes()
    }
}
