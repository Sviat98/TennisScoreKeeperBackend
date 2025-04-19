package com.bashkevich.tennisscorekeeperbackend.feature.player

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerBodyDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
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
           val playerBodyDto = call.receive<PlayerBodyDto>()

            val newPlayer = playerService.addPlayer(playerSurname = playerBodyDto.surname, playerName = playerBodyDto.name)

            call.respond(HttpStatusCode.Created, newPlayer)
        }
    }
}