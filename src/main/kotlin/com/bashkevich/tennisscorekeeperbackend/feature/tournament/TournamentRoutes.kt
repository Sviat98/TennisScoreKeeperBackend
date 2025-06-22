package com.bashkevich.tennisscorekeeperbackend.feature.tournament

import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentRequestDto
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentStatusBody
import com.bashkevich.tennisscorekeeperbackend.plugins.receiveBodyCatching
import com.bashkevich.tennisscorekeeperbackend.plugins.respondWithMessageBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.tournamentRoutes(){
    val tournamentService by application.inject<TournamentService>()

    route("/tournaments"){
        get {
            val tournaments = tournamentService.getTournaments()

            call.respond(tournaments)
        }
        post {
            val tournamentRequestDto = call.receiveBodyCatching<TournamentRequestDto>()

            val newTournament = tournamentService.addTournament(tournamentRequestDto = tournamentRequestDto)

            call.respond(status = HttpStatusCode.Created, message = newTournament)
        }
        route("/{id}"){
            get{
                val tournamentId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                val tournament = tournamentService.getTournamentById(tournamentId)

                call.respond(tournament)
            }
            patch("/status"){
                val tournamentId = call.pathParameters["id"]?.toIntOrNull() ?: 0

                val tournamentStatusBody = call.receiveBodyCatching<TournamentStatusBody>()

                tournamentService.updateTournamentStatus(tournamentId,tournamentStatusBody)

                call.respondWithMessageBody(message ="Successfully updated status to ${tournamentStatusBody.status}")
            }
        }
    }
}