package com.bashkevich.tennisscorekeeperbackend.model.match

import kotlinx.serialization.Serializable

@Serializable
data class MatchStatusBody(
    val status: MatchStatus
)
