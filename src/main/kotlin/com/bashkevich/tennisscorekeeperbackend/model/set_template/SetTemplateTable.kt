package com.bashkevich.tennisscorekeeperbackend.model.set_template

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable


object SetTemplateTable: IntIdTable("set_template") {
    val name = varchar("name",150)
    val gamesToWin = integer("games_to_win")
    val decidingPoint = bool("deciding_point")
    val tiebreakMode = enumerationByName("tiebreak_mode", 50, TiebreakMode::class)
    val tiebreakPointsToWin = integer("tiebreak_points_to_win")
    val isRegularSet = bool("is_regular")
    val isDecidingSet = bool("is_deciding")
}

