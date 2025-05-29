package com.bashkevich.tennisscorekeeperbackend.feature.participant

import com.bashkevich.tennisscorekeeperbackend.plugins.receiveMultipartCatching
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.participantRoutes(){

    val participantServiceRouter by application.inject<ParticipantServiceRouter>()

    route("/tournaments/{id}/participants"){
        post {

        }
        post("/upload") {
            val tournamentId = call.pathParameters["id"]?.toIntOrNull() ?: 0

            val file = call.receiveMultipartCatching()

            val participants = participantServiceRouter.uploadParticipants(tournamentId, file)

            call.respond(HttpStatusCode.Created,participants)
        }
    }
}