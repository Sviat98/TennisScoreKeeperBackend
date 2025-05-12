package com.bashkevich.tennisscorekeeperbackend.model.match.doubles

import com.bashkevich.tennisscorekeeperbackend.model.match.MatchStatus
import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantTable
import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantTable
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateTable
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentTable
import com.bashkevich.tennisscorekeeperbackend.plugins.MATCH_SEQUENCE
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Sequence
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.nextIntVal

object DoublesMatchTable : IdTable<Int>("doubles_match") {
    override val id = integer("id").defaultExpression(
        // Используем существующий sequence
        Sequence(MATCH_SEQUENCE).nextIntVal()
    ).entityId()
    val tournament = reference("tournament_id", TournamentTable)
    val firstParticipant = reference("first_participant_id", DoublesParticipantTable)
    val firstParticipantDisplayName = varchar("first_participant_display_name", 100)
    val secondParticipant = reference("second_participant_id", DoublesParticipantTable)
    val secondParticipantDisplayName = varchar("second_participant_display_name", 100)
    val status = enumerationByName("status", 50, MatchStatus::class)
    val firstServe = reference("first_serve_participant_id", DoublesParticipantTable).nullable()
    val firstServeInFirstPair = reference("first_serve_in_first_pair_player_id", PlayerTable).nullable()
    val firstServeInSecondPair = reference("first_serve_in_second_pair_player_id", PlayerTable).nullable()
    val setsToWin = integer("sets_to_win")
    val regularSet = reference("regular_set_id", SetTemplateTable)
    val decidingSet = reference("deciding_set_id", SetTemplateTable)
    val pointShift = integer("point_shift")
    val winner = reference("winner_participant_id", DoublesParticipantTable).nullable()

    override val primaryKey = PrimaryKey(id)
}