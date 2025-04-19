package com.bashkevich.tennisscorekeeperbackend.feature.match

import com.bashkevich.tennisscorekeeperbackend.model.match.MatchLogEvent
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchLogTable
import com.bashkevich.tennisscorekeeperbackend.model.match.ScoreType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

class MatchLogRepository {

    fun insertMatchLogEvent(
        matchLogEvent: MatchLogEvent,
    ) {
        MatchLogTable.insert {
            it[matchId] = matchLogEvent.matchId
            it[setNumber] = matchLogEvent.setNumber
            it[pointNumber] = matchLogEvent.pointNumber
            it[scoreType] = matchLogEvent.scoreType
            it[currentServe] = matchLogEvent.currentServe
            it[firstPlayerPoints] = matchLogEvent.firstPlayerPoints
            it[secondPlayerPoints] = matchLogEvent.secondPlayerPoints
        }
    }

    fun getLastPoint(
        matchId: Int,
    ) : MatchLogEvent? {
        return MatchLogTable.selectAll().where { MatchLogTable.matchId eq matchId }.orderBy(
            MatchLogTable.pointNumber,
            SortOrder.DESC
        ).limit(1).map {
            MatchLogEvent(
                matchId = it[MatchLogTable.matchId].value,
                setNumber = it[MatchLogTable.setNumber],
                pointNumber = it[MatchLogTable.pointNumber],
                scoreType = it[MatchLogTable.scoreType],
                currentServe = it[MatchLogTable.currentServe].value,
                firstPlayerPoints = it[MatchLogTable.firstPlayerPoints],
                secondPlayerPoints = it[MatchLogTable.secondPlayerPoints]
            )
        }.singleOrNull()
    }

    fun getCurrentSet(
        matchId: Int,
        setNumber: Int
    ) : MatchLogEvent? {
        return MatchLogTable.selectAll().where { (MatchLogTable.matchId eq matchId) and (MatchLogTable.scoreType eq ScoreType.GAME) and (MatchLogTable.setNumber eq setNumber) }.orderBy(
            MatchLogTable.pointNumber,
            SortOrder.DESC
        ).limit(1).map {
            MatchLogEvent(
                matchId = it[MatchLogTable.matchId].value,
                setNumber = it[MatchLogTable.setNumber],
                pointNumber = it[MatchLogTable.pointNumber],
                scoreType = it[MatchLogTable.scoreType],
                currentServe = it[MatchLogTable.currentServe].value,
                firstPlayerPoints = it[MatchLogTable.firstPlayerPoints],
                secondPlayerPoints = it[MatchLogTable.secondPlayerPoints]
            )
        }.singleOrNull()
    }

    fun getPreviousSets(matchId: Int) : List<MatchLogEvent> {
        return MatchLogTable.selectAll().where { (MatchLogTable.matchId eq matchId) and (MatchLogTable.scoreType eq ScoreType.SET) }.orderBy(
            MatchLogTable.setNumber
        ).map {
            MatchLogEvent(
                matchId = it[MatchLogTable.matchId].value,
                setNumber = it[MatchLogTable.setNumber],
                pointNumber = it[MatchLogTable.pointNumber],
                scoreType = it[MatchLogTable.scoreType],
                currentServe = it[MatchLogTable.currentServe].value,
                firstPlayerPoints = it[MatchLogTable.firstPlayerPoints],
                secondPlayerPoints = it[MatchLogTable.secondPlayerPoints]
            )
        }
    }
}