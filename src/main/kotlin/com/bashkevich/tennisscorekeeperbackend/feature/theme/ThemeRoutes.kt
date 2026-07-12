package com.bashkevich.tennisscorekeeperbackend.feature.theme

import com.bashkevich.tennisscorekeeperbackend.model.auth.JWT_AUTH
import com.bashkevich.tennisscorekeeperbackend.model.theme.ThemeBody
import com.bashkevich.tennisscorekeeperbackend.model.theme.ThemeDto
import com.bashkevich.tennisscorekeeperbackend.plugins.receiveBodyCatching
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.put
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.ktor.ext.inject

@OptIn(ExperimentalKtorApi::class)
fun Route.themeRoutes() {
    val themeService by application.inject<ThemeService>()

    route("/themes") {
        /**
         * Tag: Theme
         * Get all themes.
         */
        get {
            val themes = themeService.getThemes()

            call.respond(themes)
        }.describe {
            responses {
                HttpStatusCode.OK {
                    description = "Successfully retrieved all themes"
                    schema = jsonSchema<List<ThemeDto>>()
                }
            }
        }
        authenticate(JWT_AUTH) {
            /**
             * Tag: Theme
             * Create a new theme.
             */
            post {
                val themeBody = call.receiveBodyCatching<ThemeBody>()

                val newTheme = themeService.createTheme(
                    name = themeBody.name,
                    content = themeBody.content
                )

                call.respond(HttpStatusCode.Created, newTheme)
            }.describe {
                requestBody {
                    description = "Theme data to create"
                    schema = jsonSchema<ThemeBody>()
                }
                responses {
                    HttpStatusCode.Created {
                        description = "Theme created successfully"
                        schema = jsonSchema<ThemeDto>()
                    }
                    HttpStatusCode.BadRequest {
                        description = "Invalid request body"
                        ContentType.Text.Plain()
                    }
                    HttpStatusCode.Unauthorized {
                        description = "Missing or invalid JWT token"
                        ContentType.Text.Plain()
                    }
                }
            }
        }
        route("/{id}") {
            /**
             * Tag: Theme
             * Get theme by ID.
             */
            get {
                val id = call.pathParameters["id"]?.toIntOrNull() ?: 0

                val theme = themeService.getThemeById(id)

                call.respond(theme)
            }.describe {
                responses {
                    HttpStatusCode.OK {
                        description = "Successfully retrieved theme"
                        schema = jsonSchema<ThemeDto>()
                    }
                    HttpStatusCode.BadRequest {
                        description = "Invalid theme ID"
                        ContentType.Text.Plain()
                    }
                    HttpStatusCode.NotFound {
                        description = "Theme not found"
                        ContentType.Text.Plain()
                    }
                }
            }
            authenticate(JWT_AUTH) {
                /**
                 * Tag: Theme
                 * Update an existing theme.
                 */
                put {
                    val id = call.pathParameters["id"]?.toIntOrNull() ?: 0

                    val themeBody = call.receiveBodyCatching<ThemeBody>()

                    val updatedTheme = themeService.updateTheme(
                        id = id,
                        name = themeBody.name,
                        content = themeBody.content
                    )

                    call.respond(HttpStatusCode.OK, updatedTheme)
                }.describe {
                    requestBody {
                        description = "Updated theme data"
                        schema = jsonSchema<ThemeBody>()
                    }
                    responses {
                        HttpStatusCode.OK {
                            description = "Theme updated successfully"
                            schema = jsonSchema<ThemeDto>()
                        }
                        HttpStatusCode.BadRequest {
                            description = "Invalid request body or theme ID"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.Unauthorized {
                            description = "Missing or invalid JWT token"
                            ContentType.Text.Plain()
                        }
                        HttpStatusCode.NotFound {
                            description = "Theme not found"
                            ContentType.Text.Plain()
                        }
                    }
                }
            }
        }
    }
}
