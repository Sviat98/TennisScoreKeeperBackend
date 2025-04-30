package com.bashkevich.tennisscorekeeperbackend.model.match

data class MatchEntity (
    val id: Int,
    val firstPlayer: PlayerInMatchEntity,
    val secondPlayer: PlayerInMatchEntity,
    val status: MatchStatus,
    val firstPlayerServe: Int?,
    val setsToWin: Int,
    val regularSet: Int,
    val decidingSet: Int,
    val pointShift: Int,
    val winner: Int?
)

data class PlayerInMatchEntity(
    val id: Int,
    val displayName: String
)