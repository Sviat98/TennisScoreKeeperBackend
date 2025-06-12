package com.bashkevich.tennisscorekeeperbackend.feature.match_log

import com.bashkevich.tennisscorekeeperbackend.model.match_log.singles.SinglesMatchLogEvent
import com.bashkevich.tennisscorekeeperbackend.model.match_log.singles.SinglesMatchLogTable
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ScoreType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.greater
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
//import org.jetbrains.exposed.v1.r2dbc.Query
//import org.jetbrains.exposed.v1.r2dbc.selectAll as selectAll2


class SinglesMatchLogRepository {

    suspend fun insertMatchLogEvent(
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

    suspend fun getLastPoint(
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

    suspend fun getCurrentSet(
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

//    suspend fun getPreviousSets2(
//        matchId: Int,
//        lastPointNumber: Int,
//    ): List<SinglesMatchLogEvent> {
//        val something = SinglesMatchLogTable.selectAll2()
//            .where { (SinglesMatchLogTable.matchId eq matchId) and (SinglesMatchLogTable.scoreType eq ScoreType.SET) and (SinglesMatchLogTable.pointNumber lessEq lastPointNumber) }
//            .orderBy(
//                SinglesMatchLogTable.setNumber
//            ).toList().map {
//                SinglesMatchLogEvent(
//                    matchId = it[SinglesMatchLogTable.matchId].value,
//                    setNumber = it[SinglesMatchLogTable.setNumber],
//                    pointNumber = it[SinglesMatchLogTable.pointNumber].value,
//                    scoreType = it[SinglesMatchLogTable.scoreType],
//                    currentServe = it[SinglesMatchLogTable.currentServe]?.value,
//                    firstParticipantPoints = it[SinglesMatchLogTable.firstParticipantPoints],
//                    secondParticipantPoints = it[SinglesMatchLogTable.secondParticipantPoints]
//                )
//            }
//
//        return something
//    }

    suspend fun getPreviousSets(
        matchId: Int,
        lastPointNumber: Int,
    ): List<SinglesMatchLogEvent> {
        return SinglesMatchLogTable.selectAll()
            .where { (SinglesMatchLogTable.matchId eq matchId) and (SinglesMatchLogTable.scoreType eq ScoreType.SET) and (SinglesMatchLogTable.pointNumber lessEq lastPointNumber) }
            .orderBy(
                SinglesMatchLogTable.setNumber
            )

            .map {
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

    suspend fun removeEvents(matchId: Int, pointNumber: Int): Int {
        return SinglesMatchLogTable.deleteWhere { (SinglesMatchLogTable.matchId eq matchId) and (SinglesMatchLogTable.pointNumber greater pointNumber) }
    }
}