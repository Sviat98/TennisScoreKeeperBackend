package com.bashkevich.tennisscorekeeperbackend.feature.match.singles

import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchStatus
import com.bashkevich.tennisscorekeeperbackend.model.match.doubles.DoublesMatchTable
import com.bashkevich.tennisscorekeeperbackend.model.match.singles.SinglesMatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.match.singles.SinglesMatchTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update


class SinglesMatchRepository {
    suspend fun addMatch(tournamentId: Int, matchBody: MatchBody) = SinglesMatchTable.insertAndGetId {
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
        SinglesMatchEntity.find({ SinglesMatchTable.tournament eq tournamentId }).toList()

    fun getMatchById(id: Int) = SinglesMatchEntity.findById(id)

    suspend fun updateServe(matchId: Int, firstServeParticipantId: Int) =
        SinglesMatchTable.update({ SinglesMatchTable.id eq matchId }) {
            it[firstServingParticipant] = firstServeParticipantId
        }

    suspend fun updatePointShift(matchId: Int, newPointShift: Int) =
        SinglesMatchTable.update({ SinglesMatchTable.id eq matchId }) {
            it[pointShift] = newPointShift
        }

    suspend fun updateVideoLink(matchId: Int, videoId: String) =
        DoublesMatchTable.update({ DoublesMatchTable.id eq matchId }) {
            it[videoLink] = videoId
        }

    suspend fun updateWinner(matchId: Int, winnerParticipantId: Int?) =
        SinglesMatchTable.update({ SinglesMatchTable.id eq matchId }) {
            it[winnerParticipant] = winnerParticipantId
        }

    suspend fun setParticipantRetired(matchId: Int, retiredParticipantId: Int?) =
        SinglesMatchTable.update({ SinglesMatchTable.id eq matchId }) {
            it[retiredParticipant] = retiredParticipantId
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