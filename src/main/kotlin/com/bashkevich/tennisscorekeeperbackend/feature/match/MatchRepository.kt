package com.bashkevich.tennisscorekeeperbackend.feature.match

import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class MatchRepository {
    fun addMatch(matchBody: MatchBody) = MatchTable.insert {
        it[firstPlayer] = matchBody.firstPlayerId.toInt()
        it[secondPlayer] = matchBody.secondPlayerId.toInt()
        it[setsToWin] = matchBody.setsToWin
        it[regularSet]  = matchBody.regularSet.toInt()
        it[decidingSet] = matchBody.decidingSet.toInt()
        it[pointShift] = 0
    }

    fun getMatchById(id: Int) = MatchTable.selectAll().where{
        MatchTable.id eq id
    }.map {
        MatchEntity(
            id = id,
            firstPlayerId = it[MatchTable.firstPlayer].value,
            secondPlayerId = it[MatchTable.secondPlayer].value,
            firstPlayerServe = it[MatchTable.firstServe]?.value,
            setsToWin = it[MatchTable.setsToWin],
            regularSet = it[MatchTable.regularSet].value,
            decidingSet = it[MatchTable.decidingSet].value,
            pointShift = it[MatchTable.pointShift]
        )
    }.firstOrNull()

    fun updateServe(matchId: Int,firstServePlayerId: Int) = MatchTable.update({ MatchTable.id eq  matchId }) {
        it[firstServe] = firstServePlayerId
    }

    fun updatePointShift(matchId: Int,newPointShift: Int) = MatchTable.update({ MatchTable.id eq  matchId }) {
        it[pointShift] = newPointShift
    }

}