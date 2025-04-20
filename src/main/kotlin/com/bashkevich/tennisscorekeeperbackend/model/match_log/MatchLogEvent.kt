package com.bashkevich.tennisscorekeeperbackend.model.match_log

import com.bashkevich.tennisscorekeeperbackend.model.match.ScoreType

data class MatchLogEvent(
    val matchId: Int,
    val setNumber: Int,
    val pointNumber: Int,
    val scoreType: ScoreType,
    val currentServe: Int,
    val firstPlayerPoints: Int,
    val secondPlayerPoints: Int
)