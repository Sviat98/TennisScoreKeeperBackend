package com.bashkevich.tennisscorekeeperbackend.model.match_log

import com.bashkevich.tennisscorekeeperbackend.model.match.ScoreType

abstract class MatchLogEvent(
    open val matchId: Int,
    open val setNumber: Int,
    open val pointNumber: Int,
    open val scoreType: ScoreType,
    open val currentServe: Int,
    open val firstPlayerPoints: Int,
    open val secondPlayerPoints: Int
)