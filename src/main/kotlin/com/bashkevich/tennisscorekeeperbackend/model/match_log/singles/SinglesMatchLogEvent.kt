package com.bashkevich.tennisscorekeeperbackend.model.match_log.singles

import com.bashkevich.tennisscorekeeperbackend.model.match.ScoreType

//data class SinglesMatchLogEvent(
//    override val matchId: Int,
//    override val setNumber: Int,
//    override val pointNumber: Int,
//    override val scoreType: ScoreType,
//    override val currentServe: Int,
//    override val firstPlayerPoints: Int,
//    override val secondPlayerPoints: Int
//) : MatchLogEvent(matchId,setNumber,pointNumber,scoreType,currentServe,firstPlayerPoints,secondPlayerPoints)

data class SinglesMatchLogEvent(
    val matchId: Int,
    val setNumber: Int,
    val pointNumber: Int,
    val scoreType: ScoreType,
    val currentServe: Int,
    val firstParticipantPoints: Int,
    val secondParticipantPoints: Int
)