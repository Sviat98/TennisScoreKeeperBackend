package com.bashkevich.tennisscorekeeperbackend.feature.match_log

import com.bashkevich.tennisscorekeeperbackend.model.match.ScoreType
import com.bashkevich.tennisscorekeeperbackend.model.match_log.doubles.DoublesMatchLogEvent
import com.bashkevich.tennisscorekeeperbackend.model.match_log.doubles.DoublesMatchLogTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

class DoublesMatchLogRepository {
    fun insertMatchLogEvent(
        doublesMatchLogEvent: DoublesMatchLogEvent,
    ) {
        DoublesMatchLogTable.insert {
            it[matchId] = doublesMatchLogEvent.matchId
            it[setNumber] = doublesMatchLogEvent.setNumber
            it[pointNumber] = doublesMatchLogEvent.pointNumber
            it[scoreType] = doublesMatchLogEvent.scoreType
            it[currentServe] = doublesMatchLogEvent.currentServe
            it[currentServePlayer] = doublesMatchLogEvent.currentServeInPair
            it[firstPlayerPoints] = doublesMatchLogEvent.firstParticipantPoints
            it[secondPlayerPoints] = doublesMatchLogEvent.secondParticipantPoints
        }
    }

    fun getLastPoint(
        matchId: Int,
        lastPointNumber: Int? = null,
    ): DoublesMatchLogEvent? {
        val query = DoublesMatchLogTable.selectAll().where { DoublesMatchLogTable.matchId eq matchId }

        lastPointNumber?.let {
            query.andWhere { DoublesMatchLogTable.pointNumber lessEq lastPointNumber }
        }

        return query.orderBy(
            DoublesMatchLogTable.pointNumber,
            SortOrder.DESC
        ).limit(1).map {
            DoublesMatchLogEvent(
                matchId = it[DoublesMatchLogTable.matchId].value,
                setNumber = it[DoublesMatchLogTable.setNumber],
                pointNumber = it[DoublesMatchLogTable.pointNumber].value,
                scoreType = it[DoublesMatchLogTable.scoreType],
                currentServe = it[DoublesMatchLogTable.currentServe]?.value,
                currentServeInPair = it[DoublesMatchLogTable.currentServePlayer]?.value,
                firstParticipantPoints = it[DoublesMatchLogTable.firstPlayerPoints],
                secondParticipantPoints = it[DoublesMatchLogTable.secondPlayerPoints]
            )
        }.singleOrNull()
    }

    fun getCurrentSet(
        matchId: Int,
        setNumber: Int,
        lastPointNumber: Int,
    ): DoublesMatchLogEvent? {
        return DoublesMatchLogTable.selectAll()
            .where { (DoublesMatchLogTable.matchId eq matchId) and (DoublesMatchLogTable.scoreType eq ScoreType.GAME) and (DoublesMatchLogTable.setNumber eq setNumber) and (DoublesMatchLogTable.pointNumber lessEq lastPointNumber) }
            .orderBy(
                DoublesMatchLogTable.pointNumber,
                SortOrder.DESC
            ).limit(1).map {
                DoublesMatchLogEvent(
                    matchId = it[DoublesMatchLogTable.matchId].value,
                    setNumber = it[DoublesMatchLogTable.setNumber],
                    pointNumber = it[DoublesMatchLogTable.pointNumber].value,
                    scoreType = it[DoublesMatchLogTable.scoreType],
                    currentServe = it[DoublesMatchLogTable.currentServe]?.value,
                    currentServeInPair = it[DoublesMatchLogTable.currentServePlayer]?.value,
                    firstParticipantPoints = it[DoublesMatchLogTable.firstPlayerPoints],
                    secondParticipantPoints = it[DoublesMatchLogTable.secondPlayerPoints]
                )
            }.singleOrNull()
    }

    fun getPreviousSets(
        matchId: Int,
        lastPointNumber: Int,
    ): List<DoublesMatchLogEvent> {
        return DoublesMatchLogTable.selectAll()
            .where { (DoublesMatchLogTable.matchId eq matchId) and (DoublesMatchLogTable.scoreType eq ScoreType.SET) and (DoublesMatchLogTable.pointNumber lessEq lastPointNumber) }
            .orderBy(
                DoublesMatchLogTable.setNumber
            ).map {
                DoublesMatchLogEvent(
                    matchId = it[DoublesMatchLogTable.matchId].value,
                    setNumber = it[DoublesMatchLogTable.setNumber],
                    pointNumber = it[DoublesMatchLogTable.pointNumber].value,
                    scoreType = it[DoublesMatchLogTable.scoreType],
                    currentServe = it[DoublesMatchLogTable.currentServe]?.value,
                    currentServeInPair = it[DoublesMatchLogTable.currentServePlayer]?.value,
                    firstParticipantPoints = it[DoublesMatchLogTable.firstPlayerPoints],
                    secondParticipantPoints = it[DoublesMatchLogTable.secondPlayerPoints]
                )
            }
    }

    fun removeEvents(matchId: Int, pointNumber: Int): Int {
        return DoublesMatchLogTable.deleteWhere { (DoublesMatchLogTable.matchId eq matchId) and (DoublesMatchLogTable.pointNumber greater pointNumber) }
    }
}