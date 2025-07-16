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
    POINT, //обычный розыгрыш
    TIEBREAK_POINT,// розыгрыш на тай-брейке либо супер-тай-брейке
    GAME, // гейм
    SET, // сет (который не привел к победе в матче)
    RETIREMENT_FIRST, // снятие первого участника матча (фиксируется текущий счет сета)
    RETIREMENT_SECOND, // снятие второго участника матча (фиксируется текущий счет сета)
    FINAL_SET_FIRST, // сет, который привел к победе в матче первого участника
    FINAL_SET_SECOND // сет, который привел к победе в матче второго участника
}
