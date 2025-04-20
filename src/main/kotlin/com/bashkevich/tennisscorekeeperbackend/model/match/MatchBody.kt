package com.bashkevich.tennisscorekeeperbackend.model.match

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatchBody(
    @SerialName("first_player_id")
    val firstPlayerId: Int,
    @SerialName("second_player_id")
    val secondPlayerId: Int,
    @SerialName("sets_to_win")
    val setsToWin: Int,
    @SerialName("regular_set_id")
    val regularSet: Int,
    @SerialName("deciding_set_id")
    val decidingSet: Int
)
