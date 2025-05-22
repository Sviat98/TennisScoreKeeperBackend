package com.bashkevich.tennisscorekeeperbackend.feature.match_log

import com.bashkevich.tennisscorekeeperbackend.model.match_log.singles.SinglesMatchLogEvent
import com.bashkevich.tennisscorekeeperbackend.model.match_log.singles.SinglesMatchLogTable
import com.bashkevich.tennisscorekeeperbackend.model.match.ScoreType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

class SinglesMatchLogRepository {

    fun insertMatchLogEvent(
        singlesMatchLogEvent: SinglesMatchLogEvent,
    ) {
        SinglesMatchLogTable.insert {
            it[matchId] = singlesMatchLogEvent.matchId
            it[setNumber] = singlesMatchLogEvent.setNumber
            it[pointNumber] = singlesMatchLogEvent.pointNumber
            it[scoreType] = singlesMatchLogEvent.scoreType
            it[currentServe] = singlesMatchLogEvent.currentServe
            it[firstParticipantPoints] = singlesMatchLogEvent.firstParticipantPoints
            it[secondParticipantPoints] = singlesMatchLogEvent.secondParticipantPoints
        }
    }

    fun getLastPoint(
        matchId: Int,
        lastPointNumber: Int? = null,
    ): SinglesMatchLogEvent? {
        val query = SinglesMatchLogTable.selectAll().where { SinglesMatchLogTable.matchId eq matchId }

        lastPointNumber?.let {
            query.andWhere { SinglesMatchLogTable.pointNumber lessEq lastPointNumber }
        }

        return query.orderBy(
            SinglesMatchLogTable.pointNumber,
            SortOrder.DESC
        ).limit(1).map {
            SinglesMatchLogEvent(
                matchId = it[SinglesMatchLogTable.matchId].value,
                setNumber = it[SinglesMatchLogTable.setNumber],
                pointNumber = it[SinglesMatchLogTable.pointNumber].value,
                scoreType = it[SinglesMatchLogTable.scoreType],
                currentServe = it[SinglesMatchLogTable.currentServe]?.value,
                firstParticipantPoints = it[SinglesMatchLogTable.firstParticipantPoints],
                secondParticipantPoints = it[SinglesMatchLogTable.secondParticipantPoints]
            )
        }.singleOrNull()
    }

    fun getCurrentSet(
        matchId: Int,
        setNumber: Int,
        lastPointNumber: Int,
    ): SinglesMatchLogEvent? {
        return SinglesMatchLogTable.selectAll()
            .where { (SinglesMatchLogTable.matchId eq matchId) and (SinglesMatchLogTable.scoreType eq ScoreType.GAME) and (SinglesMatchLogTable.setNumber eq setNumber) and (SinglesMatchLogTable.pointNumber lessEq lastPointNumber) }
            .orderBy(
                SinglesMatchLogTable.pointNumber,
                SortOrder.DESC
            ).limit(1).map {
                SinglesMatchLogEvent(
                    matchId = it[SinglesMatchLogTable.matchId].value,
                    setNumber = it[SinglesMatchLogTable.setNumber],
                    pointNumber = it[SinglesMatchLogTable.pointNumber].value,
                    scoreType = it[SinglesMatchLogTable.scoreType],
                    currentServe = it[SinglesMatchLogTable.currentServe]?.value,
                    firstParticipantPoints = it[SinglesMatchLogTable.firstParticipantPoints],
                    secondParticipantPoints = it[SinglesMatchLogTable.secondParticipantPoints]
                )
            }.singleOrNull()
    }

    fun getPreviousSets(
        matchId: Int,
        lastPointNumber: Int,
    ): List<SinglesMatchLogEvent> {
        return SinglesMatchLogTable.selectAll()
            .where { (SinglesMatchLogTable.matchId eq matchId) and (SinglesMatchLogTable.scoreType eq ScoreType.SET) and (SinglesMatchLogTable.pointNumber lessEq lastPointNumber) }
            .orderBy(
                SinglesMatchLogTable.setNumber
            ).map {
                SinglesMatchLogEvent(
                    matchId = it[SinglesMatchLogTable.matchId].value,
                    setNumber = it[SinglesMatchLogTable.setNumber],
                    pointNumber = it[SinglesMatchLogTable.pointNumber].value,
                    scoreType = it[SinglesMatchLogTable.scoreType],
                    currentServe = it[SinglesMatchLogTable.currentServe]?.value,
                    firstParticipantPoints = it[SinglesMatchLogTable.firstParticipantPoints],
                    secondParticipantPoints = it[SinglesMatchLogTable.secondParticipantPoints]
                )
            }
    }

    fun removeEvents(matchId: Int, pointNumber: Int): Int {
        return SinglesMatchLogTable.deleteWhere { (SinglesMatchLogTable.matchId eq matchId) and (SinglesMatchLogTable.pointNumber greater pointNumber) }
    }
}