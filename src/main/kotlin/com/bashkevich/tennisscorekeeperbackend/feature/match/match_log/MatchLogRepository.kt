package com.bashkevich.tennisscorekeeperbackend.feature.match.match_log

import com.bashkevich.tennisscorekeeperbackend.model.match_log.MatchLogEvent
import com.bashkevich.tennisscorekeeperbackend.model.match_log.MatchLogTable
import com.bashkevich.tennisscorekeeperbackend.model.match.ScoreType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
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
        lastPointNumber: Int? = null,
    ): MatchLogEvent? {
        val query = MatchLogTable.selectAll().where { MatchLogTable.matchId eq matchId }

        lastPointNumber?.let {
            query.andWhere { MatchLogTable.pointNumber lessEq lastPointNumber }
        }

        return query.orderBy(
            MatchLogTable.pointNumber,
            SortOrder.DESC
        ).limit(1).map {
            MatchLogEvent(
                matchId = it[MatchLogTable.matchId].value,
                setNumber = it[MatchLogTable.setNumber],
                pointNumber = it[MatchLogTable.pointNumber].value,
                scoreType = it[MatchLogTable.scoreType],
                currentServe = it[MatchLogTable.currentServe].value,
                firstPlayerPoints = it[MatchLogTable.firstPlayerPoints],
                secondPlayerPoints = it[MatchLogTable.secondPlayerPoints]
            )
        }.singleOrNull()
    }

    fun getCurrentSet(
        matchId: Int,
        setNumber: Int,
        lastPointNumber: Int,
    ): MatchLogEvent? {
        return MatchLogTable.selectAll()
            .where { (MatchLogTable.matchId eq matchId) and (MatchLogTable.scoreType eq ScoreType.GAME) and (MatchLogTable.setNumber eq setNumber) and (MatchLogTable.pointNumber lessEq lastPointNumber) }
            .orderBy(
                MatchLogTable.pointNumber,
                SortOrder.DESC
            ).limit(1).map {
                MatchLogEvent(
                    matchId = it[MatchLogTable.matchId].value,
                    setNumber = it[MatchLogTable.setNumber],
                    pointNumber = it[MatchLogTable.pointNumber].value,
                    scoreType = it[MatchLogTable.scoreType],
                    currentServe = it[MatchLogTable.currentServe].value,
                    firstPlayerPoints = it[MatchLogTable.firstPlayerPoints],
                    secondPlayerPoints = it[MatchLogTable.secondPlayerPoints]
                )
            }.singleOrNull()
    }

    fun getPreviousSets(
        matchId: Int,
        lastPointNumber: Int,
    ): List<MatchLogEvent> {
        return MatchLogTable.selectAll()
            .where { (MatchLogTable.matchId eq matchId) and (MatchLogTable.scoreType eq ScoreType.SET) and (MatchLogTable.pointNumber lessEq lastPointNumber) }
            .orderBy(
                MatchLogTable.setNumber
            ).map {
                MatchLogEvent(
                    matchId = it[MatchLogTable.matchId].value,
                    setNumber = it[MatchLogTable.setNumber],
                    pointNumber = it[MatchLogTable.pointNumber].value,
                    scoreType = it[MatchLogTable.scoreType],
                    currentServe = it[MatchLogTable.currentServe].value,
                    firstPlayerPoints = it[MatchLogTable.firstPlayerPoints],
                    secondPlayerPoints = it[MatchLogTable.secondPlayerPoints]
                )
            }
    }

    fun removeEvents(matchId: Int, pointNumber: Int): Int {
        return MatchLogTable.deleteWhere { (MatchLogTable.matchId eq matchId) and (MatchLogTable.pointNumber greater pointNumber) }
    }
}