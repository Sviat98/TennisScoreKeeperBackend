package com.bashkevich.tennisscorekeeperbackend.model.match_log

import com.bashkevich.tennisscorekeeperbackend.model.match.MatchTable
import com.bashkevich.tennisscorekeeperbackend.model.match.ScoreType
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable
import org.jetbrains.exposed.dao.id.IntIdTable

object MatchLogTable: IntIdTable("match_log") {
    val matchId = reference("match_id", MatchTable)
    val setNumber = integer("set_number")
    val pointNumber = integer("point_number")
    val currentServe = reference("now_serving", PlayerTable)
    val scoreType = enumerationByName("score_type", 50, ScoreType::class)
    val firstPlayerPoints = integer("first_player_points")
    val secondPlayerPoints = integer("second_player_points")
}