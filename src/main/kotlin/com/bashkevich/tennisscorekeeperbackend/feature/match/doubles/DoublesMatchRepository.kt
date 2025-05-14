package com.bashkevich.tennisscorekeeperbackend.feature.match.doubles

import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchStatus
import com.bashkevich.tennisscorekeeperbackend.model.match.doubles.DoublesMatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.match.doubles.DoublesMatchTable
import com.bashkevich.tennisscorekeeperbackend.model.match.singles.SinglesMatchTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update

class DoublesMatchRepository {
    fun addMatch(tournamentId: Int,matchBody: MatchBody) = DoublesMatchTable.insertAndGetId {
        it[tournament] = tournamentId
        it[firstParticipant] = matchBody.firstParticipant.id.toInt()
        it[firstParticipantDisplayName] = matchBody.firstParticipant.displayName
        it[secondParticipant] = matchBody.secondParticipant.id.toInt()
        it[secondParticipantDisplayName] = matchBody.secondParticipant.displayName
        it[status] = MatchStatus.NOT_STARTED
        it[setsToWin] = matchBody.setsToWin
        it[regularSet] = matchBody.regularSet.toInt()
        it[decidingSet] = matchBody.decidingSet.toInt()
        it[pointShift] = 0
        it[winner] = null
    }

    fun getMatches(tournamentId: Int) = DoublesMatchEntity.find({ DoublesMatchTable.tournament eq tournamentId}).toList()

    fun getMatchById(id: Int) = DoublesMatchEntity.findById(id)

    fun updateServe(matchId: Int, firstServeParticipantId: Int) =
        DoublesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[firstServe] = firstServeParticipantId
        }

    fun updateServeInFirstPair(matchId: Int, firstServePlayerId: Int) =
        DoublesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[firstServeInFirstPair] = firstServePlayerId
        }

    fun updateServeInSecondPair(matchId: Int, firstServePlayerId: Int) =
        DoublesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[firstServeInSecondPair] = firstServePlayerId
        }

    fun updatePointShift(matchId: Int, newPointShift: Int) =
        DoublesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[pointShift] = newPointShift
        }

    fun updateWinner(matchId: Int, winnerParticipantId: Int?) =
        DoublesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[winner] = winnerParticipantId
        }
}