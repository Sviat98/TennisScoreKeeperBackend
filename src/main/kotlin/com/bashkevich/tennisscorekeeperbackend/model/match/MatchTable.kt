package com.bashkevich.tennisscorekeeperbackend.model.match

import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantTable
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateTable
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

abstract class MatchTable(name: String = ""): IntIdTable(name){
    val firstPlayer = reference("first_player_id", SinglesParticipantTable)
    val firstPlayerDisplayName = varchar("first_player_display_name", 50)
    val secondPlayer = reference("second_player_id", SinglesParticipantTable)
    val secondPlayerDisplayName = varchar("second_player_display_name", 50)
    val status = enumerationByName("status", 50, MatchStatus::class)
    val firstServe = reference("first_serve_player_id", SinglesParticipantTable).nullable()
    val setsToWin = integer("sets_to_win")
    val regularSet = reference("regular_set_id", SetTemplateTable)
    val decidingSet = reference("deciding_set_id", SetTemplateTable)
    val pointShift = integer("point_shift")
    val winner = reference("winner_player_id", SinglesParticipantTable).nullable()
}
