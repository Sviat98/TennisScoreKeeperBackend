package com.bashkevich.tennisscorekeeperbackend.feature.match_log


//abstract class BaseMatchLogRepository<T : MatchLogEvent, Table : MatchLogTable>(
//    private val table: Table,
//    private val mapToEvent: (ResultRow) -> T,
//) {
//    fun insertMatchLogEvent(event: T) {
//        table.insert {
//            it[matchId] = event.matchId
//            it[setNumber] = event.setNumber
//            it[pointNumber] = event.pointNumber
//            it[scoreType] = event.scoreType
//            it[currentServe] = event.currentServe
//            if (event is DoublesMatchLogEvent) {
//                it[currentServePlayer] = event.currentServeInPair
//            }
//            it[firstPlayerPoints] = event.firstPlayerPoints
//            it[secondPlayerPoints] = event.secondPlayerPoints
//        }
//    }
//
//    fun getLastPoint(matchId: Int, lastPointNumber: Int? = null): T? {
//        val query = table.selectAll().where { table.matchId eq matchId }
//        lastPointNumber?.let { query.andWhere { table.pointNumber lessEq it } }
//        return query
//            .orderBy(table.pointNumber, SortOrder.DESC)
//            .limit(1)
//            .map(mapToEvent)
//            .singleOrNull()
//    }
//
//    fun getCurrentSet(matchId: Int, setNumber: Int, lastPointNumber: Int): T? {
//        return table.selectAll()
//            .where {
//                (table.matchId eq matchId) and
//                        (table.scoreType eq ScoreType.GAME) and
//                        (table.setNumber eq setNumber) and
//                        (table.pointNumber lessEq lastPointNumber)
//            }
//            .orderBy(table.pointNumber, SortOrder.DESC)
//            .limit(1)
//            .map(mapToEvent)
//            .singleOrNull()
//    }
//
//    fun getPreviousSets(matchId: Int, lastPointNumber: Int): List<T> {
//        return table.selectAll()
//            .where {
//                (table.matchId eq matchId) and
//                        (table.scoreType eq ScoreType.SET) and
//                        (table.pointNumber lessEq lastPointNumber)
//            }
//            .orderBy(table.setNumber)
//            .map(mapToEvent)
//    }
//
//    fun removeEvents(matchId: Int, pointNumber: Int): Int {
//        return table.deleteWhere {
//            (table.matchId eq matchId) and
//                    (table.pointNumber greater pointNumber)
//        }
//    }
//}