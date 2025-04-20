package com.bashkevich.tennisscorekeeperbackend.model.match

data class MatchEntity (
    val id: Int,
    val firstPlayerId: Int,
    val secondPlayerId: Int,
    val firstPlayerServe: Int?,
    val setsToWin: Int,
    val regularSet: Int,
    val decidingSet: Int,
)