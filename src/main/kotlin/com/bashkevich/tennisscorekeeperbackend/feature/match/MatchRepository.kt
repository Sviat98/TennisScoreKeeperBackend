package com.bashkevich.tennisscorekeeperbackend.feature.match

import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterEntity
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchLogEvent
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchLogTable
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchTable
import com.bashkevich.tennisscorekeeperbackend.model.match.ScoreType
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class MatchRepository {
    fun addMatch(firstPlayerId: Int,secondPlayerId: Int) = MatchTable.insert {
        it[firstPlayer] = firstPlayerId
        it[secondPlayer] = secondPlayerId
    }

    fun getMatchById(id: Int) = MatchTable.selectAll().where{
        MatchTable.id eq id
    }.map {
        MatchEntity(
            firstPlayerId = it[MatchTable.firstPlayer].value,
            secondPlayerId = it[MatchTable.secondPlayer].value,
            firstPlayerServe = it[MatchTable.firstServe]?.value
        )
    }.firstOrNull()

    fun updateServe(matchId: Int,firstServePlayerId: Int) = MatchTable.update({ MatchTable.id eq  matchId }) {
        it[firstServe] = firstServePlayerId
    }


}