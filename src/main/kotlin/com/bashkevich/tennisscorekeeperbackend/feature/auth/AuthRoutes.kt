package com.bashkevich.tennisscorekeeperbackend.feature.auth

import com.bashkevich.tennisscorekeeperbackend.model.auth.LoginInfo
import com.bashkevich.tennisscorekeeperbackend.plugins.respondWithMessageBody
import io.ktor.server.request.receive
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject

fun Route.authRoutes() {
    val authService by application.inject<AuthService>()

    post("/login") {
        val loginInfo = call.receive<LoginInfo>()

        val loginResponse = authService.login(loginInfo.login, loginInfo.password)

        call.respond(loginResponse)
    }

    post("/logout") {
        val formParameters = call.receiveParameters()

        val refreshToken = formParameters["refreshToken"]

        authService.logout(refreshToken)

        call.respondWithMessageBody(message = "Successfully logged out")
    }

    get("/refreshToken") {
        val formParameters = call.receiveParameters()

        val refreshToken = formParameters["refreshToken"]

        val loginResponse = authService.refreshAccessToken(refreshToken)

        call.respond(loginResponse)
    }

    get("/refreshTokenStatus") {
        val formParameters = call.receiveParameters()

        val refreshToken = formParameters["refreshToken"]

        authService.checkRefreshTokenIsValid(refreshToken)

        call.respondWithMessageBody(message = "Your token is valid")
    }

}