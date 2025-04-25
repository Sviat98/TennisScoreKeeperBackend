package com.bashkevich.tennisscorekeeperbackend.model.match

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServeBody(
    @SerialName("serving_player_id")
    val servingPlayerId: String
)
