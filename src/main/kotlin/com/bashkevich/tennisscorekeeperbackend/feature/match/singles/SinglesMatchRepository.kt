package com.bashkevich.tennisscorekeeperbackend.feature.match.singles

import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchStatus
import com.bashkevich.tennisscorekeeperbackend.model.match.singles.SinglesMatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.match.singles.SinglesMatchTable
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
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
        it[regularSet] = matchBody.regularSet.toInt()
        it[decidingSet] = matchBody.decidingSet.toInt()
        it[pointShift] = 0
        it[winner] = null
    }

//    fun addMatch1(tournamentId: Int, matchBody: MatchBody) = SinglesMatchTable.insert() {
//        it[tournament] = tournamentId
//        it[firstParticipant] = matchBody.firstParticipant.id.toInt()
//        it[firstParticipantDisplayName] = matchBody.firstParticipant.displayName
//        it[secondParticipant] = matchBody.secondParticipant.id.toInt()
//        it[secondParticipantDisplayName] = matchBody.secondParticipant.displayName
//        it[status] = MatchStatus.NOT_STARTED
//        it[setsToWin] = matchBody.setsToWin
//        it[regularSet] = matchBody.regularSet.toInt()
//        it[decidingSet] = matchBody.decidingSet.toInt()
//        it[pointShift] = 0
//        it[winner] = null
//    }

    fun getMatches(tournamentId: Int) = SinglesMatchEntity.find({ SinglesMatchTable.tournament eq tournamentId}).toList()

//    fun getMatches() = SinglesMatchTable.leftJoin(
//        otherTable = FirstPlayer,
//        onColumn = { SinglesMatchTable.firstParticipant },
//        otherColumn = { FirstPlayer[PlayerTable.id] })
//        .leftJoin(
//            otherTable = SecondPlayer,
//            onColumn = { SinglesMatchTable.secondParticipant },
//            otherColumn = { SecondPlayer[PlayerTable.id] }).selectAll().map {
//            ShortMatchEntity(
//                id = it[SinglesMatchTable.id].value,
//                firstPlayer = PlayerInShortMatchEntity(
//                    id = it[FirstPlayer[PlayerTable.id]].value,
//                    surname = it[FirstPlayer[PlayerTable.surname]],
//                    name = it[FirstPlayer[PlayerTable.name]]
//                ),
//                secondPlayer = PlayerInShortMatchEntity(
//                    id = it[SecondPlayer[PlayerTable.id]].value,
//                    surname = it[SecondPlayer[PlayerTable.surname]],
//                    name = it[SecondPlayer[PlayerTable.name]]
//                ),
//                status = it[SinglesMatchTable.status],
//                winnerPlayerId = it[SinglesMatchTable.winner]?.value
//            )
//        }

//    fun getShortMatchById(id: Int) = SinglesMatchTable.leftJoin(
//        otherTable = FirstPlayer,
//        onColumn = { SinglesMatchTable.firstParticipant },
//        otherColumn = { FirstPlayer[PlayerTable.id] })
//        .leftJoin(
//            otherTable = SecondPlayer,
//            onColumn = { SinglesMatchTable.secondParticipant },
//            otherColumn = { SecondPlayer[PlayerTable.id] }).selectAll().where {
//            SinglesMatchTable.id eq id
//        }.map {
//            ShortMatchEntity(
//                id = it[SinglesMatchTable.id].value,
//                firstPlayer = PlayerInShortMatchEntity(
//                    id = it[FirstPlayer[PlayerTable.id]].value,
//                    surname = it[FirstPlayer[PlayerTable.surname]],
//                    name = it[FirstPlayer[PlayerTable.name]]
//                ),
//                secondPlayer = PlayerInShortMatchEntity(
//                    id = it[SecondPlayer[PlayerTable.id]].value,
//                    surname = it[SecondPlayer[PlayerTable.surname]],
//                    name = it[SecondPlayer[PlayerTable.name]]
//                ),
//                status = it[SinglesMatchTable.status],
//                winnerPlayerId = it[SinglesMatchTable.winner]?.value
//            )
//        }.firstOrNull()

//    fun getMatchById(id: Int) = SinglesMatchTable.selectAll().where {
//        SinglesMatchTable.id eq id
//    }.map {
//        MatchEntity(
//            id = id,
//            firstPlayer = PlayerInMatchEntity(
//                id = it[SinglesMatchTable.firstParticipant].value,
//                displayName = it[SinglesMatchTable.firstParticipantDisplayName]
//            ),
//            secondPlayer = PlayerInMatchEntity(
//                id = it[SinglesMatchTable.secondParticipant].value,
//                displayName = it[SinglesMatchTable.secondParticipantDisplayName]
//            ),
//            status = it[SinglesMatchTable.status],
//            firstPlayerServe = it[SinglesMatchTable.firstServe]?.value,
//            setsToWin = it[SinglesMatchTable.setsToWin],
//            regularSet = it[SinglesMatchTable.regularSet].value,
//            decidingSet = it[SinglesMatchTable.decidingSet].value,
//            pointShift = it[SinglesMatchTable.pointShift],
//            winner = it[SinglesMatchTable.winner]?.value,
//        )
//    }.firstOrNull()

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
}