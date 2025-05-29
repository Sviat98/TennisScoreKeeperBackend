package com.bashkevich.tennisscorekeeperbackend.model.match_log


//open class MatchLogTable(name: String = "") : CompositeIdTable(name) {
//    val matchId = reference("match_id", MatchTable)
//    val pointNumber = integer("point_number").entityId()
//    val setNumber = integer("set_number")
//    val currentServe = reference("now_serving", PlayerTable)
//    val scoreType = enumerationByName("score_type", 50, ScoreType::class)
//    val firstPlayerPoints = integer("first_player_points")
//    val secondPlayerPoints = integer("second_player_points")
//
//    init {
//        addIdColumn(matchId)
//    }
//
//    override val primaryKey = PrimaryKey(matchId, pointNumber)
//}