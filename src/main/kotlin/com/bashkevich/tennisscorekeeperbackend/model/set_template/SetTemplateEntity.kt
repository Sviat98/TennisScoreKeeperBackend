package com.bashkevich.tennisscorekeeperbackend.model.set_template

data class SetTemplateEntity(
    val id: Int,
    val gamesToWin: Int,
    val decidingPoint: Boolean,
    val tiebreakMode: TiebreakMode,
    val tiebreakPointsToWin: Int
)
