package com.bashkevich.tennisscorekeeperbackend.model.match.body

import com.bashkevich.tennisscorekeeperbackend.model.match.MatchStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatchStatusBody(
    @SerialName("status")
    val status: MatchStatus
)
