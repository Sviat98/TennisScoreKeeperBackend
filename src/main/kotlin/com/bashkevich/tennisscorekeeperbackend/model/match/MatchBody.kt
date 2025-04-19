package com.bashkevich.tennisscorekeeperbackend.model.match

import kotlinx.serialization.Serializable

@Serializable
data class MatchBody(
    val firstPlayerId: Int,
    val secondPlayerId: Int
)
