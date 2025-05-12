package com.bashkevich.tennisscorekeeperbackend.model.match

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatchBody(
    @SerialName("first_participant")
    val firstParticipant: ParticipantBody,
    @SerialName("second_participant")
    val secondParticipant: ParticipantBody,
    @SerialName("sets_to_win")
    val setsToWin: Int,
    @SerialName("regular_set_id")
    val regularSet: String,
    @SerialName("deciding_set_id")
    val decidingSet: String
)

@Serializable
data class ParticipantBody(
    @SerialName("id")
    val id: String,
    @SerialName("display_name")
    val displayName: String,
)
