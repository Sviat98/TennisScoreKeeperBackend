package com.bashkevich.tennisscorekeeperbackend.model.match.singles

import com.bashkevich.tennisscorekeeperbackend.model.match.MatchStatus
import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantTable
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateTable
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentTable
import com.bashkevich.tennisscorekeeperbackend.plugins.MATCH_SEQUENCE
import com.bashkevich.tennisscorekeeperbackend.plugins.PARTICIPANT_SEQUENCE
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Sequence
import org.jetbrains.exposed.sql.nextIntVal

object SinglesMatchTable: IdTable<Int>("singles_match"){
    override val id = integer("id").defaultExpression(
        // Используем существующий sequence
        Sequence(MATCH_SEQUENCE).nextIntVal()
    ).entityId()
    val tournament = reference("tournament_id", TournamentTable)
    val firstParticipant = reference("first_participant_id", SinglesParticipantTable)
    val firstParticipantDisplayName = varchar("first_participant_display_name", 50)
    val secondParticipant = reference("second_participant_id", SinglesParticipantTable)
    val secondParticipantDisplayName = varchar("second_participant_display_name", 50)
    val status = enumerationByName("status", 50, MatchStatus::class)
    val firstServe = reference("first_serve_player_id", SinglesParticipantTable).nullable()
    val setsToWin = integer("sets_to_win")
    val regularSet = reference("regular_set_id", SetTemplateTable)
    val decidingSet = reference("deciding_set_id", SetTemplateTable)
    val pointShift = integer("point_shift")
    val winner = reference("winner_player_id", SinglesParticipantTable).nullable()

    override val primaryKey = PrimaryKey(id)
}

