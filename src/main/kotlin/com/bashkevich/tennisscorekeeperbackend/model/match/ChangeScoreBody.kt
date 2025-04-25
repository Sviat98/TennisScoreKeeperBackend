package com.bashkevich.tennisscorekeeperbackend.model.match

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChangeScoreBody(
    @SerialName("player_id")
    val playerId: String,
    @SerialName("score_type")
    val scoreType: ScoreType,
)

enum class ScoreType{
    POINT,TIEBREAK_POINT,GAME,SET
}
