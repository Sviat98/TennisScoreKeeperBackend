package com.bashkevich.tennisscorekeeperbackend.feature.tournament

import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentDto
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentRequestDto
import com.bashkevich.tennisscorekeeperbackend.model.tournament.toDto
import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException

class TournamentService (
    private val tournamentRepository: TournamentRepository
){
    suspend fun getTournaments(): List<TournamentDto>{
        return dbQuery {
            tournamentRepository.getTournaments().map { it.toDto() }
        }
    }

    suspend fun addTournament(tournamentRequestDto: TournamentRequestDto): TournamentDto {
        return dbQuery {
            tournamentRepository.addTournament(tournamentRequestDto).toDto()
        }
    }

    suspend fun getTournamentById(tournamentId: Int): TournamentDto {
        return dbQuery {
            if (tournamentId == 0) throw BadRequestException("Wrong format of tournament id")

            val tournament = tournamentRepository.getTournamentById(tournamentId)
                ?: throw NotFoundException("No tournament found by that id")

            tournament.toDto()
        }
    }

}
