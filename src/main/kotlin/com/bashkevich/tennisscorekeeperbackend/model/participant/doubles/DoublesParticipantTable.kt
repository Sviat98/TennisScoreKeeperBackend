package com.bashkevich.tennisscorekeeperbackend.model.participant.doubles

import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantTable
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentTable
import com.bashkevich.tennisscorekeeperbackend.plugins.PARTICIPANT_SEQUENCE
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Sequence
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.nextIntVal

object DoublesParticipantTable: IdTable<Int>("doubles_participant") {
    override val id = integer("id").defaultExpression(
        // Используем существующий sequence
        Sequence(PARTICIPANT_SEQUENCE).nextIntVal()
    ).entityId()
    val tournament = reference("tournament_id", TournamentTable)
    val firstPlayer = reference("first_player_id", PlayerTable)
    val secondPlayer = reference("second_player_id", PlayerTable)
    val seed = integer("seed").nullable()

    override val primaryKey = PrimaryKey(id)
}