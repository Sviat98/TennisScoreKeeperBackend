package com.bashkevich.tennisscorekeeperbackend.feature.match.doubles

import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchStatus
import com.bashkevich.tennisscorekeeperbackend.model.match.doubles.DoublesMatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.match.doubles.DoublesMatchTable
import com.bashkevich.tennisscorekeeperbackend.model.match.singles.SinglesMatchTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update


class DoublesMatchRepository {
    suspend fun addMatch(tournamentId: Int, matchBody: MatchBody) = DoublesMatchTable.insertAndGetId {
        it[tournament] = tournamentId
        it[firstParticipant] = matchBody.firstParticipant.id.toInt()
        it[firstParticipantDisplayName] = matchBody.firstParticipant.displayName
        it[firstParticipantPrimaryColor] = matchBody.firstParticipant.primaryColor
        it[firstParticipantSecondaryColor] = matchBody.firstParticipant.secondaryColor
        it[secondParticipant] = matchBody.secondParticipant.id.toInt()
        it[secondParticipantDisplayName] = matchBody.secondParticipant.displayName
        it[secondParticipantPrimaryColor] = matchBody.secondParticipant.primaryColor
        it[secondParticipantSecondaryColor] = matchBody.secondParticipant.secondaryColor
        it[setsToWin] = matchBody.setsToWin
        it[regularSetTemplate] = matchBody.regularSet?.toInt()
        it[decidingSetTemplate] = matchBody.decidingSet.toInt()
    }

    fun getMatches(tournamentId: Int) =
        DoublesMatchEntity.find({ DoublesMatchTable.tournament eq tournamentId }).toList()

    fun getMatchById(id: Int) = DoublesMatchEntity.findById(id)

    suspend fun updateServe(matchId: Int, firstServeParticipantId: Int) =
        DoublesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[firstServingParticipant] = firstServeParticipantId
        }

    suspend fun updateServeInFirstPair(matchId: Int, firstServePlayerId: Int) =
        DoublesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[firstServingPlayerInFirstPair] = firstServePlayerId
        }

    suspend fun updateServeInSecondPair(matchId: Int, firstServePlayerId: Int) =
        DoublesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[firstServingPlayerInSecondPair] = firstServePlayerId
        }

    suspend fun updatePointShift(matchId: Int, newPointShift: Int) =
        DoublesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[pointShift] = newPointShift
        }

    suspend fun updateWinner(matchId: Int, winnerParticipantId: Int?) =
        DoublesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[winnerParticipant] = winnerParticipantId
        }

    suspend fun setParticipantRetired(matchId: Int, retiredParticipantId: Int?) =
        SinglesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[retiredParticipant] = retiredParticipantId
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