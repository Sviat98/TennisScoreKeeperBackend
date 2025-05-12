package com.bashkevich.tennisscorekeeperbackend.feature.tournament

import com.bashkevich.tennisscorekeeperbackend.model.match.doubles.DoublesMatchTable
import com.bashkevich.tennisscorekeeperbackend.model.match.singles.SinglesMatchTable
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentEntity
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentRequestDto
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentStatus
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentTable
import org.jetbrains.exposed.sql.unionAll

class TournamentRepository {

    fun addTournament(tournamentRequestDto: TournamentRequestDto) =
        TournamentEntity.new {
            name = tournamentRequestDto.name
            type = tournamentRequestDto.type
            status = TournamentStatus.NOT_STARTED
        }

    fun getTournamentById(id: Int) = TournamentEntity.findById(id)

    fun getTournamentByMatchId(matchId: Int): TournamentEntity? {
        val query = (TournamentTable innerJoin SinglesMatchTable).select(TournamentTable.columns)
            .where { SinglesMatchTable.id eq matchId }
            .unionAll(
                (TournamentTable innerJoin DoublesMatchTable).select(TournamentTable.columns)
                    .where { DoublesMatchTable.id eq matchId }
            )

        return TournamentEntity.wrapRows(query).firstOrNull()
    }

}