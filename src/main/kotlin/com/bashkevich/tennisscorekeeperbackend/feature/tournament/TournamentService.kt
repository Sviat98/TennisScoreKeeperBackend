package com.bashkevich.tennisscorekeeperbackend.feature.tournament

import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentDto
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentRequestDto
import com.bashkevich.tennisscorekeeperbackend.model.tournament.toDto
import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery

class TournamentService (
    private val tournamentRepository: TournamentRepository
){
    suspend fun addTournament(tournamentRequestDto: TournamentRequestDto): TournamentDto {
        return dbQuery {
            tournamentRepository.addTournament(tournamentRequestDto).toDto()
        }
    }

}
