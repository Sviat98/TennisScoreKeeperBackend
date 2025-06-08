package com.bashkevich.tennisscorekeeperbackend.model.set_template

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SetTemplateDto(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("games_to_win")
    val gamesToWin: Int,
    @SerialName("has_deciding_point")
    val hasDecidingPoint: Boolean,
    @SerialName("tiebreak_mode")
    val tiebreakMode: TiebreakMode,
    @SerialName("tiebreak_points_to_win")
    val tiebreakPointsToWin: Int,
    @SerialName("is_regular")
    val isRegular: Boolean,
    @SerialName("is_deciding")
    val isDeciding: Boolean,
)

fun SetTemplateEntity.toDto() = SetTemplateDto(
    id = this.id.toString(),
    name = this.name,
    gamesToWin = this.gamesToWin,
    hasDecidingPoint = this.decidingPoint,
    tiebreakMode = this.tiebreakMode,
    tiebreakPointsToWin = this.tiebreakPointsToWin,
    isRegular = this.isRegularSet,
    isDeciding = this.isDecidingSet
)
