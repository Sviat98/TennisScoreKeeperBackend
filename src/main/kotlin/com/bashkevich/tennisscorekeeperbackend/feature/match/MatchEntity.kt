package com.bashkevich.tennisscorekeeperbackend.feature.match

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable

data class MatchEntity (
    val firstPlayerId: Int,
    val secondPlayerId: Int,
    val firstPlayerServe: Int?
)