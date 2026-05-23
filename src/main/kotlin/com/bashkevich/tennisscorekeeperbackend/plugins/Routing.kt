package com.bashkevich.tennisscorekeeperbackend.plugins

import com.bashkevich.tennisscorekeeperbackend.feature.auth.authRoutes
import com.bashkevich.tennisscorekeeperbackend.feature.counter.counterRoutes
import com.bashkevich.tennisscorekeeperbackend.feature.match.matchRoutes
import com.bashkevich.tennisscorekeeperbackend.feature.participant.participantRoutes
import com.bashkevich.tennisscorekeeperbackend.feature.player.playerRoutes
import com.bashkevich.tennisscorekeeperbackend.feature.set_template.setTemplateRoutes
import com.bashkevich.tennisscorekeeperbackend.feature.theme.themeRoutes
import com.bashkevich.tennisscorekeeperbackend.feature.tournament.tournamentRoutes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Application.configureRouting() {
    routing {
        /**
         * Tag: System
         * Root endpoint, returns greeting.
         */
        get("/") {
            call.respondText("Hello World!")
        }.describe {
            responses {
                HttpStatusCode.OK {
                    description = "Server is running"
                    ContentType.Text.Plain()
                }
            }
        }
        /**
         * Tag: System
         * Check database connectivity.
         */
        get("/dbHealth") {
            // проверка доступности БД
            if (isDbConnected()) call.respond("OK")
            else call.respond(HttpStatusCode.ServiceUnavailable)
        }.describe {
            responses {
                HttpStatusCode.OK {
                    description = "Database is connected and healthy"
                    ContentType.Text.Plain()
                }
                HttpStatusCode.ServiceUnavailable {
                    description = "Database is not available"
                    ContentType.Text.Plain()
                }
            }
        }
        authRoutes()
        counterRoutes()
        tournamentRoutes()
        playerRoutes()
        matchRoutes()
        setTemplateRoutes()
        themeRoutes()
        participantRoutes()
    }
}
