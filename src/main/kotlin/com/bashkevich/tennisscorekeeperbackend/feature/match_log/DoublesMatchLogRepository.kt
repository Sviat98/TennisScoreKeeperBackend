package com.bashkevich.tennisscorekeeperbackend.feature.match_log

import com.bashkevich.tennisscorekeeperbackend.model.match.body.ScoreType
import com.bashkevich.tennisscorekeeperbackend.model.match_log.doubles.DoublesMatchLogEvent
import com.bashkevich.tennisscorekeeperbackend.model.match_log.doubles.DoublesMatchLogTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class DoublesMatchLogRepository {
    suspend fun insertMatchLogEvent(
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

    suspend fun getLastPoint(
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
        ).limit(1).map { it.toDoublesMatchLogEvent() }.singleOrNull()
    }

    suspend fun getCurrentSet(
        matchId: Int,
        setNumber: Int,
        lastPointNumber: Int,
    ): DoublesMatchLogEvent? {
        return DoublesMatchLogTable.selectAll()
            .where { (DoublesMatchLogTable.matchId eq matchId) and (DoublesMatchLogTable.scoreType eq ScoreType.GAME) and (DoublesMatchLogTable.setNumber eq setNumber) and (DoublesMatchLogTable.pointNumber lessEq lastPointNumber) }
            .orderBy(
                DoublesMatchLogTable.pointNumber,
                SortOrder.DESC
            ).limit(1).map { it.toDoublesMatchLogEvent() }.singleOrNull()
    }

    suspend fun getPreviousSets(
        matchId: Int,
        lastPointNumber: Int,
    ): List<DoublesMatchLogEvent> {
        return DoublesMatchLogTable.selectAll()
            .where { (DoublesMatchLogTable.matchId eq matchId) and (DoublesMatchLogTable.scoreType inList listOf(
                ScoreType.SET,
                ScoreType.FINAL_SET_FIRST, ScoreType.FINAL_SET_SECOND,
                ScoreType.RETIREMENT_FIRST, ScoreType.RETIREMENT_SECOND
            )) and (DoublesMatchLogTable.pointNumber lessEq lastPointNumber) }
            .orderBy(
                DoublesMatchLogTable.setNumber
            )
            .map { it.toDoublesMatchLogEvent() }
    }

    // первый эффективный розыгрыш (POINT/TIEBREAK_POINT) сета
    suspend fun getFirstRallyInSet(
        matchId: Int,
        setNumber: Int,
        lastPointNumber: Int,
    ): DoublesMatchLogEvent? =
        DoublesMatchLogTable.selectAll()
            .where {
                (DoublesMatchLogTable.matchId eq matchId) and
                        (DoublesMatchLogTable.setNumber eq setNumber) and
                        (DoublesMatchLogTable.scoreType inList listOf(ScoreType.POINT, ScoreType.TIEBREAK_POINT)) and
                        (DoublesMatchLogTable.pointNumber lessEq lastPointNumber)
            }
            .orderBy(DoublesMatchLogTable.pointNumber, SortOrder.ASC)
            .limit(1)
            .map { it.toDoublesMatchLogEvent() }
            .singleOrNull()

    // первый завершенный гейм (GAME) сета
    suspend fun getFirstGameInSet(
        matchId: Int,
        setNumber: Int,
        lastPointNumber: Int,
    ): DoublesMatchLogEvent? =
        DoublesMatchLogTable.selectAll()
            .where {
                (DoublesMatchLogTable.matchId eq matchId) and
                        (DoublesMatchLogTable.setNumber eq setNumber) and
                        (DoublesMatchLogTable.scoreType eq ScoreType.GAME) and
                        (DoublesMatchLogTable.pointNumber lessEq lastPointNumber)
            }
            .orderBy(DoublesMatchLogTable.pointNumber, SortOrder.ASC)
            .limit(1)
            .map { it.toDoublesMatchLogEvent() }
            .singleOrNull()

    // конкретная строка лога (в частности, следующая строка redo-хвоста)
    suspend fun getLogRow(
        matchId: Int,
        pointNumber: Int,
    ): DoublesMatchLogEvent? =
        DoublesMatchLogTable.selectAll()
            .where {
                (DoublesMatchLogTable.matchId eq matchId) and
                        (DoublesMatchLogTable.pointNumber eq pointNumber)
            }
            .limit(1)
            .map { it.toDoublesMatchLogEvent() }
            .singleOrNull()

    // мутация проекции подачи в строке лога (ручная смена подачи в паре на граничной позиции)
    suspend fun updateServingPlayerInRow(matchId: Int, pointNumber: Int, playerId: Int) {
        DoublesMatchLogTable.update({
            (DoublesMatchLogTable.matchId eq matchId) and (DoublesMatchLogTable.pointNumber eq pointNumber)
        }) {
            it[currentServePlayer] = playerId
        }
    }

    suspend fun removeEvents(matchId: Int, pointNumber: Int): Int {
        return DoublesMatchLogTable.deleteWhere { (DoublesMatchLogTable.matchId eq matchId) and (DoublesMatchLogTable.pointNumber greater pointNumber) }
    }

    private fun ResultRow.toDoublesMatchLogEvent() = DoublesMatchLogEvent(
        matchId = this[DoublesMatchLogTable.matchId].value,
        setNumber = this[DoublesMatchLogTable.setNumber],
        pointNumber = this[DoublesMatchLogTable.pointNumber].value,
        scoreType = this[DoublesMatchLogTable.scoreType],
        currentServe = this[DoublesMatchLogTable.currentServe]?.value,
        currentServeInPair = this[DoublesMatchLogTable.currentServePlayer]?.value,
        firstParticipantPoints = this[DoublesMatchLogTable.firstPlayerPoints],
        secondParticipantPoints = this[DoublesMatchLogTable.secondPlayerPoints]
    )
}
