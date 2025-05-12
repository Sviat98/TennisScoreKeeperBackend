package com.bashkevich.tennisscorekeeperbackend.model.match_log.doubles

import com.bashkevich.tennisscorekeeperbackend.model.match.ScoreType
import com.bashkevich.tennisscorekeeperbackend.model.match.doubles.DoublesMatchTable
import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantTable
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable
import org.jetbrains.exposed.dao.id.CompositeIdTable

object DoublesMatchLogTable : CompositeIdTable("doubles_match_log") {
    val matchId = reference("match_id", DoublesMatchTable)
    val pointNumber = integer("point_number").entityId()
    val setNumber = integer("set_number")
    val currentServe = reference("now_serving", DoublesParticipantTable)
    val currentServePlayer = reference("now_serving_player", PlayerTable)
    val scoreType = enumerationByName("score_type", 50, ScoreType::class)
    val firstPlayerPoints = integer("first_player_points")
    val secondPlayerPoints = integer("second_player_points")

    init {
        addIdColumn(matchId)
    }

    override val primaryKey = PrimaryKey(matchId, pointNumber)
}