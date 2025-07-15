package com.bashkevich.tennisscorekeeperbackend.model.match.body

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChangeScoreBody(
    @SerialName("participant_id")
    val participantId: String,
    @SerialName("score_type")
    val scoreType: ScoreType,
)

enum class ScoreType{
    POINT,TIEBREAK_POINT,GAME, SET, RETIREMENT_FIRST, RETIREMENT_SECOND, FINAL_SET_FIRST, FINAL_SET_SECOND
}
