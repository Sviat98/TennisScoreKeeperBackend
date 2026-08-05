package com.bashkevich.tennisscorekeeperbackend.feature.theme

import com.bashkevich.tennisscorekeeperbackend.model.auth.JWT_AUTH
import com.bashkevich.tennisscorekeeperbackend.model.message.ResponseMessageDto
import com.bashkevich.tennisscorekeeperbackend.model.theme.ThemeBody
import com.bashkevich.tennisscorekeeperbackend.model.theme.ThemeDto
import com.bashkevich.tennisscorekeeperbackend.plugins.receiveBodyCatching
import com.bashkevich.tennisscorekeeperbackend.plugins.receiveMultipartCatching
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
            /**
             * Tag: Theme
             * Generate a theme from a scoreboard image via AI (Koog, OpenAI GPT-4o).
             * Returns the extracted ThemeContent JSON (no DB write at the debugging stage).
             */
            post("/ai") {
                val multipart = call.receiveMultipartCatching()

                val theme = themeService.generateThemeFromImage(multipart)

                call.respond(theme)
            }
            /**
             * Tag: Theme
             * Прежняя (до детерминированного конвейера) реализация: один LLM-вызов, GPT-4o сам
             * называет 9 цветов табло. Фолбэк для случаев, когда новый /themes/ai (с наложенной
             * сеткой) ошибочно отбраковывает валидное табло. Возвращает ThemeContent JSON.
             */
            post("/ai/old") {
                val multipart = call.receiveMultipartCatching()

                val theme = themeService.generateThemeFromImageLegacy(multipart)

                call.respond(theme)
            }
            /**
             * Tag: Theme
             * Описывает содержимое табло по загруженному изображению через AI (Koog, OpenAI GPT-4o):
             * игроки, кто подаёт, текущий счёт (сеты, текущий сет, гейм), расположение лиц и
             * ключевые цвета (название + #RRGGBB). Возвращает строку-описание на русском в ResponseMessageDto.
             * Если изображение не является табло — message = «На изображении не теннисное табло.».
             */
            post("/ai/describeMatch") {
                val multipart = call.receiveMultipartCatching()

                val description = themeService.describeMatchFromImage(multipart)

                call.respond(ResponseMessageDto(description))
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
