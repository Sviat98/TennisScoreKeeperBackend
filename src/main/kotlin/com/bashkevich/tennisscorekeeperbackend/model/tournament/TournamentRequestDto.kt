package com.bashkevich.tennisscorekeeperbackend.model.tournament

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TournamentRequestDto(
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: TournamentType,
    @SerialName("sets_to_win")
    val setsToWin: Int,
    @SerialName("regular_set_id")
    val regularSetId: String? = null,
    @SerialName("deciding_set_id")
    val decidingSetId: String,
    @SerialName("theme_id")
    val themeId: String,
)
