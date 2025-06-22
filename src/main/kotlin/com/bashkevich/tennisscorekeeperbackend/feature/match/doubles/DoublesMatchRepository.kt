package com.bashkevich.tennisscorekeeperbackend.feature.match.doubles

import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchStatus
import com.bashkevich.tennisscorekeeperbackend.model.match.doubles.DoublesMatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.match.doubles.DoublesMatchTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update


class DoublesMatchRepository {
    suspend fun addMatch(tournamentId: Int, matchBody: MatchBody) = DoublesMatchTable.insertAndGetId {
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

    fun getMatches(tournamentId: Int) =
        DoublesMatchEntity.find({ DoublesMatchTable.tournament eq tournamentId }).toList()

    fun getMatchById(id: Int) = DoublesMatchEntity.findById(id)

    suspend fun updateServe(matchId: Int, firstServeParticipantId: Int) =
        DoublesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[firstServe] = firstServeParticipantId
        }

    suspend fun updateServeInFirstPair(matchId: Int, firstServePlayerId: Int) =
        DoublesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[firstServeInFirstPair] = firstServePlayerId
        }

    suspend fun updateServeInSecondPair(matchId: Int, firstServePlayerId: Int) =
        DoublesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[firstServeInSecondPair] = firstServePlayerId
        }

    suspend fun updatePointShift(matchId: Int, newPointShift: Int) =
        DoublesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[pointShift] = newPointShift
        }

    suspend fun updateWinner(matchId: Int, winnerParticipantId: Int?) =
        DoublesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[winner] = winnerParticipantId
        }

    suspend fun updateStatus(matchId: Int, matchStatus: MatchStatus) {
        DoublesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[status] = matchStatus
        }
    }

    suspend fun getIncompletedMatchesAmount(tournamentId: Int): Int =
        DoublesMatchTable.selectAll()
            .where { (DoublesMatchTable.tournament eq tournamentId) and (DoublesMatchTable.status neq MatchStatus.COMPLETED) }
            .count().toInt()
}