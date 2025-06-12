package com.bashkevich.tennisscorekeeperbackend.model.match_log.singles

import com.bashkevich.tennisscorekeeperbackend.model.match.body.ScoreType
import com.bashkevich.tennisscorekeeperbackend.model.match.singles.SinglesMatchTable
import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantTable
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable

object SinglesMatchLogTable : CompositeIdTable("singles_match_log") {
    val matchId = reference("match_id", SinglesMatchTable)
    val pointNumber = integer("point_number").entityId()
    val setNumber = integer("set_number")
    val currentServe = reference("now_serving", SinglesParticipantTable).nullable()
    val scoreType = enumerationByName("score_type", 50, ScoreType::class)
    val firstParticipantPoints = integer("first_participant_points")
    val secondParticipantPoints = integer("second_participant_points")

    init {
        addIdColumn(matchId)
    }

    override val primaryKey = PrimaryKey(matchId, pointNumber)
}