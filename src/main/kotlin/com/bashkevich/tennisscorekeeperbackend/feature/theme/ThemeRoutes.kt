package com.bashkevich.tennisscorekeeperbackend.feature.theme

import com.bashkevich.tennisscorekeeperbackend.model.auth.JWT_AUTH
import com.bashkevich.tennisscorekeeperbackend.model.theme.ThemeBody
import com.bashkevich.tennisscorekeeperbackend.plugins.receiveBodyCatching
import com.bashkevich.tennisscorekeeperbackend.plugins.respondWithMessageBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.themeRoutes() {
    val themeService by application.inject<ThemeService>()

    route("/themes") {
        get {
            val themes = themeService.getThemes()

            call.respond(themes)
        }
        authenticate(JWT_AUTH) {
            post {
                val themeBody = call.receiveBodyCatching<ThemeBody>()

                val newTheme = themeService.createTheme(
                    name = themeBody.name,
                    content = themeBody.content
                )

                call.respond(HttpStatusCode.Created, newTheme)
            }
        }
        route("/{id}") {
            get {
                val id = call.pathParameters["id"]?.toIntOrNull() ?: 0

                val theme = themeService.getThemeById(id)

                call.respond(theme)
            }
            authenticate(JWT_AUTH) {
                put {
                    val id = call.pathParameters["id"]?.toIntOrNull() ?: 0

                    val themeBody = call.receiveBodyCatching<ThemeBody>()

                    themeService.updateTheme(
                        id = id,
                        name = themeBody.name,
                        content = themeBody.content
                    )

                    call.respondWithMessageBody(message = "Successfully updated theme")
                }
            }
        }
    }
}
