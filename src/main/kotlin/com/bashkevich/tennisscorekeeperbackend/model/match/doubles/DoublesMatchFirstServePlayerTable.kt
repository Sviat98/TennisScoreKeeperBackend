package com.bashkevich.tennisscorekeeperbackend.model.match.doubles

import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantTable
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable

object DoublesMatchFirstServePlayerTable : CompositeIdTable("doubles_match_first_serve_player") {
    val match = reference("match_id", DoublesMatchTable)
    val participant = reference("participant_id", DoublesParticipantTable)
    val set = integer("set_number").entityId()
    val player = reference("player_id", PlayerTable)

    init {
        addIdColumn(match)
        addIdColumn(participant)
    }

    override val primaryKey = PrimaryKey(match, participant, set)
}
