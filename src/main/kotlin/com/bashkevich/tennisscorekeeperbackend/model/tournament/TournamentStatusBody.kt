package com.bashkevich.tennisscorekeeperbackend.model.tournament

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TournamentStatusBody(
    @SerialName("status")
    val status: TournamentStatus
)