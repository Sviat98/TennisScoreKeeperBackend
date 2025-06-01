package com.bashkevich.tennisscorekeeperbackend.model.participant.doubles

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentTable
import com.bashkevich.tennisscorekeeperbackend.plugins.PARTICIPANT_SEQUENCE
import org.jetbrains.exposed.v1.core.Sequence
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.nextIntVal


object DoublesParticipantTable : IdTable<Int>("doubles_participant") {
    override val id = integer("id").defaultExpression(
        // Используем существующий sequence
        Sequence(PARTICIPANT_SEQUENCE).nextIntVal()
    ).entityId()
    val tournament = reference("tournament_id", TournamentTable)
    val firstPlayer = reference("first_player_id", PlayerTable)
    val secondPlayer = reference("second_player_id", PlayerTable)
    val seed = integer("seed").nullable()
    val saveOrderAtDisplay =
        bool("save_order_at_display") // у нас id первого и второго игрока будут храниться по возрастанию
    //но если хотим поменять при отображении, ставим флаг равный false

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(
            customIndexName = "idx_doubles_participant_tournament_players",
            tournament, firstPlayer, secondPlayer
        )
    }
}