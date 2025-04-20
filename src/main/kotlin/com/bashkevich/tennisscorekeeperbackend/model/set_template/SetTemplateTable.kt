package com.bashkevich.tennisscorekeeperbackend.model.set_template

import org.jetbrains.exposed.dao.id.IntIdTable

object SetTemplateTable: IntIdTable("set_template") {
    val gamesToWin = integer("games_to_win")
    val decidingPoint = bool("deciding_point")
    val tiebreakMode = enumerationByName("tiebreak_mode", 50, TiebreakMode::class)
    val tiebreakPointsToWin = integer("tiebreak_points_to_win")
}

