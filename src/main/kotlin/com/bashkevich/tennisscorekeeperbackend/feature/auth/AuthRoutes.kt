package com.bashkevich.tennisscorekeeperbackend.feature.auth

import com.bashkevich.tennisscorekeeperbackend.model.auth.LoginInfo
import com.bashkevich.tennisscorekeeperbackend.model.auth.LoginResponse
import com.bashkevich.tennisscorekeeperbackend.model.auth.RefreshTokensResponse
import com.bashkevich.tennisscorekeeperbackend.model.message.ResponseMessageDto
import com.bashkevich.tennisscorekeeperbackend.plugins.respondWithMessageBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.request.receive
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.ktor.ext.inject

@OptIn(ExperimentalKtorApi::class)
fun Route.authRoutes() {
    val authService by application.inject<AuthService>()

    /**
     * Tag: Auth
     * Authenticate user with login and password. Returns JWT access and refresh tokens.
     */
    post("/login") {
        val loginInfo = call.receive<LoginInfo>()

        val loginResponse = authService.login(loginInfo.login, loginInfo.password)

        call.respond(loginResponse)
    }.describe {
        requestBody {
            description = "Login credentials"
            schema = jsonSchema<LoginInfo>()
        }
        responses {
            HttpStatusCode.OK {
                description = "Login successful, returns player data and JWT tokens"
                schema = jsonSchema<LoginResponse>()
            }
            HttpStatusCode.BadRequest {
                description = "Invalid request body format"
                ContentType.Text.Plain()
            }
            HttpStatusCode.Unauthorized {
                description = "Invalid login or password"
                ContentType.Text.Plain()
            }
        }
    }

    /**
     * Tag: Auth
     * Logout user by invalidating refresh token.
     */
    post("/logout") {
        val formParameters = call.receiveParameters()

        val refreshToken = formParameters["refreshToken"]

        authService.logout(refreshToken)

        call.respondWithMessageBody(message = "Successfully logged out")
    }.describe {
        responses {
            HttpStatusCode.OK {
                description = "Successfully logged out"
                schema = jsonSchema<ResponseMessageDto>()
            }
            HttpStatusCode.BadRequest {
                description = "Refresh token not provided"
                ContentType.Text.Plain()
            }
        }
    }

    /**
     * Tag: Auth
     * Refresh access token using refresh token. Returns new JWT access and refresh tokens.
     */
    post("/refreshToken") {
        val formParameters = call.receiveParameters()

        val refreshToken = formParameters["refreshToken"]

        val loginResponse = authService.refreshAccessToken(refreshToken)

        call.respond(loginResponse)
    }.describe {
        responses {
            HttpStatusCode.OK {
                description = "Token refreshed successfully, returns new JWT tokens"
                schema = jsonSchema<RefreshTokensResponse>()
            }
            HttpStatusCode.BadRequest {
                description = "Refresh token not provided"
                ContentType.Text.Plain()
            }
            HttpStatusCode.Unauthorized {
                description = "Invalid or expired refresh token"
                ContentType.Text.Plain()
            }
        }
    }

    /**
     * Tag: Auth
     * Check if refresh token is still valid.
     */
    post("/refreshTokenStatus") {
        val formParameters = call.receiveParameters()

        val refreshToken = formParameters["refreshToken"]

        authService.checkRefreshTokenIsValid(refreshToken)

        call.respondWithMessageBody(message = "Your token is valid")
    }.describe {
        responses {
            HttpStatusCode.OK {
                description = "Token is valid"
                schema = jsonSchema<ResponseMessageDto>()
            }
            HttpStatusCode.BadRequest {
                description = "Refresh token not provided"
                ContentType.Text.Plain()
            }
            HttpStatusCode.Unauthorized {
                description = "Invalid or expired refresh token"
                ContentType.Text.Plain()
            }
        }
    }

}
