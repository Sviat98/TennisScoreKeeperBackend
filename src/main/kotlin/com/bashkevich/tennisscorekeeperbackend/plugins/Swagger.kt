package com.bashkevich.tennisscorekeeperbackend.plugins

import io.ktor.http.ContentType
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.routingRoot

fun Application.configureSwagger() {
    routing {
        swaggerUI("/swagger") {
            info = OpenApiInfo("Tennis Score Keeper API", "1.0.0")
            source = OpenApiDocSource.Routing(ContentType.Application.Json) {
                routingRoot.descendants()
            }
        }
    }
}
