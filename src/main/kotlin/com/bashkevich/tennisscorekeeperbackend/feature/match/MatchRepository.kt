package com.bashkevich.tennisscorekeeperbackend.feature.match

import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchStatus
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchTable
import com.bashkevich.tennisscorekeeperbackend.model.match.PlayerInMatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.match.PlayerInShortMatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.match.ShortMatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.player.FirstPlayer
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable
import com.bashkevich.tennisscorekeeperbackend.model.player.SecondPlayer
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class MatchRepository {
    fun addMatch(matchBody: MatchBody) = MatchTable.insertAndGetId {
        it[firstPlayer] = matchBody.firstPlayer.id.toInt()
        it[firstPlayerDisplayName] = matchBody.firstPlayer.displayName
        it[secondPlayer] = matchBody.secondPlayer.id.toInt()
        it[secondPlayerDisplayName] = matchBody.secondPlayer.displayName
        it[status] = MatchStatus.NOT_STARTED
        it[setsToWin] = matchBody.setsToWin
        it[regularSet] = matchBody.regularSet.toInt()
        it[decidingSet] = matchBody.decidingSet.toInt()
        it[pointShift] = 0
        it[winner] = null
    }

    fun getMatches() = MatchTable.leftJoin(
        otherTable = FirstPlayer,
        onColumn = { MatchTable.firstPlayer },
        otherColumn = { FirstPlayer[PlayerTable.id] })
        .leftJoin(
            otherTable = SecondPlayer,
            onColumn = { MatchTable.secondPlayer },
            otherColumn = { SecondPlayer[PlayerTable.id] }).selectAll().map {
            ShortMatchEntity(
                id = it[MatchTable.id].value,
                firstPlayer = PlayerInShortMatchEntity(
                    id = it[FirstPlayer[PlayerTable.id]].value,
                    surname = it[FirstPlayer[PlayerTable.surname]],
                    name = it[FirstPlayer[PlayerTable.name]]
                ),
                secondPlayer = PlayerInShortMatchEntity(
                    id = it[SecondPlayer[PlayerTable.id]].value,
                    surname = it[SecondPlayer[PlayerTable.surname]],
                    name = it[SecondPlayer[PlayerTable.name]]
                ),
                status = it[MatchTable.status],
                winnerPlayerId = it[MatchTable.winner]?.value
            )
        }

    fun getShortMatchById(id: Int) = MatchTable.leftJoin(
        otherTable = FirstPlayer,
        onColumn = { MatchTable.firstPlayer },
        otherColumn = { FirstPlayer[PlayerTable.id] })
        .leftJoin(
            otherTable = SecondPlayer,
            onColumn = { MatchTable.secondPlayer },
            otherColumn = { SecondPlayer[PlayerTable.id] }).selectAll().where {
            MatchTable.id eq id
        }.map {
            ShortMatchEntity(
                id = it[MatchTable.id].value,
                firstPlayer = PlayerInShortMatchEntity(
                    id = it[FirstPlayer[PlayerTable.id]].value,
                    surname = it[FirstPlayer[PlayerTable.surname]],
                    name = it[FirstPlayer[PlayerTable.name]]
                ),
                secondPlayer = PlayerInShortMatchEntity(
                    id = it[SecondPlayer[PlayerTable.id]].value,
                    surname = it[SecondPlayer[PlayerTable.surname]],
                    name = it[SecondPlayer[PlayerTable.name]]
                ),
                status = it[MatchTable.status],
                winnerPlayerId = it[MatchTable.winner]?.value
            )
        }.firstOrNull()

    fun getMatchById(id: Int) = MatchTable.selectAll().where {
        MatchTable.id eq id
    }.map {
        MatchEntity(
            id = id,
            firstPlayer = PlayerInMatchEntity(
                id = it[MatchTable.firstPlayer].value,
                displayName = it[MatchTable.firstPlayerDisplayName]
            ),
            secondPlayer = PlayerInMatchEntity(
                id = it[MatchTable.secondPlayer].value,
                displayName = it[MatchTable.secondPlayerDisplayName]
            ),
            status = it[MatchTable.status],
            firstPlayerServe = it[MatchTable.firstServe]?.value,
            setsToWin = it[MatchTable.setsToWin],
            regularSet = it[MatchTable.regularSet].value,
            decidingSet = it[MatchTable.decidingSet].value,
            pointShift = it[MatchTable.pointShift],
            winner = it[MatchTable.winner]?.value,
        )
    }.firstOrNull()

    fun updateServe(matchId: Int, firstServePlayerId: Int) = MatchTable.update({ MatchTable.id eq matchId }) {
        it[firstServe] = firstServePlayerId
    }

    fun updatePointShift(matchId: Int, newPointShift: Int) = MatchTable.update({ MatchTable.id eq matchId }) {
        it[pointShift] = newPointShift
    }

    fun updateWinner(matchId: Int, winnerPlayerId: Int?) = MatchTable.update({ MatchTable.id eq matchId }) {
        it[winner] = winnerPlayerId
    }
}