package com.bashkevich.tennisscorekeeperbackend.model.match_log.doubles

import com.bashkevich.tennisscorekeeperbackend.model.match.ScoreType
import com.bashkevich.tennisscorekeeperbackend.model.match_log.MatchLogEvent

//data class DoublesMatchLogEvent(
//    override val matchId: Int,
//    override val setNumber: Int,
//    override val pointNumber: Int,
//    override val scoreType: ScoreType,
//    override val currentServe: Int,
//    val currentServeInPair: Int,
//    override val firstParticipantPoints: Int,
//    override val secondParticipantPoints: Int
//) : MatchLogEvent(matchId,setNumber,pointNumber,scoreType,currentServe,firstParticipantPoints,secondParticipantPoints)

data class DoublesMatchLogEvent(
    val matchId: Int,
    val setNumber: Int,
    val pointNumber: Int,
    val scoreType: ScoreType,
    val currentServe: Int,
    val currentServeInPair: Int,
    val firstParticipantPoints: Int,
    val secondParticipantPoints: Int
)