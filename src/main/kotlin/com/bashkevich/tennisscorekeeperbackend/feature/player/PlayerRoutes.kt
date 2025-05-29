package com.bashkevich.tennisscorekeeperbackend.feature.player

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerBodyDto
import com.bashkevich.tennisscorekeeperbackend.plugins.receiveBodyCatching
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.playerRoutes(){
    val playerService by application.inject<PlayerService>()

    route("/players") {
        post {
           val playerBodyDto = call.receiveBodyCatching<PlayerBodyDto>()

            val newPlayer = playerService.addPlayer(playerBodyDto = playerBodyDto)

            call.respond(HttpStatusCode.Created, newPlayer)
        }
    }
}