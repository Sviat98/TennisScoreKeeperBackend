package com.bashkevich.tennisscorekeeperbackend.model.match

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateTable
import org.jetbrains.exposed.dao.id.IntIdTable

object MatchTable: IntIdTable("match"){
    val firstPlayer = reference("first_player", PlayerTable)
    val secondPlayer = reference("second_player", PlayerTable)
    val firstServe = reference("first_serve", PlayerTable).nullable()
    val setsToWin = integer("sets_to_win")
    val regularSet = reference("regular_set", SetTemplateTable)
    val decidingSet = reference("deciding_set", SetTemplateTable)
}