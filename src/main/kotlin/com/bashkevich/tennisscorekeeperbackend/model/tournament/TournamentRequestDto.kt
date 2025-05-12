package com.bashkevich.tennisscorekeeperbackend.model.tournament

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TournamentRequestDto(
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: TournamentType,
)
