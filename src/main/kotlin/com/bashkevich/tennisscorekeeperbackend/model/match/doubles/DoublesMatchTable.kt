package com.bashkevich.tennisscorekeeperbackend.model.match.doubles

import com.bashkevich.tennisscorekeeperbackend.model.match.MatchStatus
import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantTable
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateTable
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentTable
import com.bashkevich.tennisscorekeeperbackend.plugins.MATCH_SEQUENCE
import org.jetbrains.exposed.v1.core.Sequence
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.nextIntVal


object DoublesMatchTable : IdTable<Int>("doubles_match") {
    override val id = integer("id").defaultExpression(
        // Используем существующий sequence
        Sequence(MATCH_SEQUENCE).nextIntVal()
    ).entityId()
    val tournament = reference("tournament_id", TournamentTable)
    val firstParticipant = reference("first_participant_id", DoublesParticipantTable)
    val firstParticipantDisplayName = varchar("first_participant_display_name", 100)
    val firstParticipantPrimaryColor = varchar("first_participant_primary_color", 6).default("ffffff")
    val firstParticipantSecondaryColor = varchar("first_participant_secondary_color", 6).nullable()
    val secondParticipant = reference("second_participant_id", DoublesParticipantTable)
    val secondParticipantDisplayName = varchar("second_participant_display_name", 100)
    val secondParticipantPrimaryColor = varchar("second_participant_primary_color", 6).default("ffffff")
    val secondParticipantSecondaryColor = varchar("second_participant_secondary_color", 6).nullable()
    val status = enumerationByName("status", 50, MatchStatus::class).default(MatchStatus.NOT_STARTED)
    val firstServingParticipant = reference("first_serving_participant_id", DoublesParticipantTable).nullable()
    val firstServingPlayerInFirstPair = reference("first_serve_in_first_pair_player_id", PlayerTable).nullable()
    val firstServingPlayerInSecondPair = reference("first_serve_in_second_pair_player_id", PlayerTable).nullable()
    val setsToWin = integer("sets_to_win")
    val regularSetTemplate = reference("regular_set_id", SetTemplateTable).nullable()
    val decidingSetTemplate = reference("deciding_set_id", SetTemplateTable)
    val pointShift = integer("point_shift").default(0)
    val winnerParticipant = reference("winner_participant_id", DoublesParticipantTable).nullable()
    val retiredParticipant = reference("retired_participant_id", DoublesParticipantTable).nullable()

    override val primaryKey = PrimaryKey(id)
}