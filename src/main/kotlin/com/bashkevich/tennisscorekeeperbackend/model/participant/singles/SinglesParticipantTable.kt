package com.bashkevich.tennisscorekeeperbackend.model.participant.singles

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentTable
import com.bashkevich.tennisscorekeeperbackend.plugins.PARTICIPANT_SEQUENCE
import org.jetbrains.exposed.v1.core.Sequence
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.nextIntVal


object SinglesParticipantTable: IdTable<Int>("singles_participant") {
    override val id = integer("id").defaultExpression(
        // Используем существующий sequence
        Sequence(PARTICIPANT_SEQUENCE).nextIntVal()
    ).entityId()
    val tournament = reference("tournament_id", TournamentTable)
    val player = reference("player_id", PlayerTable)
    val seed = integer("seed").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(customIndexName = "idx_singles_participant_tournament_player", tournament, player)
    }
}