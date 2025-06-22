package com.bashkevich.tennisscorekeeperbackend.feature.match.singles

import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchStatus
import com.bashkevich.tennisscorekeeperbackend.model.match.singles.SinglesMatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.match.singles.SinglesMatchTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update


class SinglesMatchRepository {
   suspend fun addMatch(tournamentId: Int, matchBody: MatchBody) = SinglesMatchTable.insertAndGetId {
        it[tournament] = tournamentId
        it[firstParticipant] = matchBody.firstParticipant.id.toInt()
        it[firstParticipantDisplayName] = matchBody.firstParticipant.displayName
        it[secondParticipant] = matchBody.secondParticipant.id.toInt()
        it[secondParticipantDisplayName] = matchBody.secondParticipant.displayName
        it[status] = MatchStatus.NOT_STARTED
        it[setsToWin] = matchBody.setsToWin
        it[regularSet] = matchBody.regularSet?.toInt()
        it[decidingSet] = matchBody.decidingSet.toInt()
        it[pointShift] = 0
        it[winner] = null
    }

    fun getMatches(tournamentId: Int) = SinglesMatchEntity.find({ SinglesMatchTable.tournament eq tournamentId}).toList()

    fun getMatchById(id: Int) = SinglesMatchEntity.findById(id)

    suspend fun updateServe(matchId: Int, firstServeParticipantId: Int) =
        SinglesMatchTable.update({ SinglesMatchTable.id eq matchId }) {
            it[firstServe] = firstServeParticipantId
        }

    suspend fun updatePointShift(matchId: Int, newPointShift: Int) =
        SinglesMatchTable.update({ SinglesMatchTable.id eq matchId }) {
            it[pointShift] = newPointShift
        }

    suspend fun updateWinner(matchId: Int, winnerParticipantId: Int?) =
        SinglesMatchTable.update({ SinglesMatchTable.id eq matchId }) {
            it[winner] = winnerParticipantId
        }

    suspend fun updateStatus(matchId: Int, matchStatus: MatchStatus) {
        SinglesMatchTable.update({ SinglesMatchTable.id eq matchId }) {
            it[status] = matchStatus
        }
    }

    suspend fun getIncompletedMatchesAmount(tournamentId: Int): Int =
        SinglesMatchTable.selectAll()
            .where { (SinglesMatchTable.tournament eq tournamentId) and (SinglesMatchTable.status neq MatchStatus.COMPLETED) }
            .count().toInt()
}