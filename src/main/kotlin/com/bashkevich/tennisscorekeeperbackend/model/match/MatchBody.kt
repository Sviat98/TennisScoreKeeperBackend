package com.bashkevich.tennisscorekeeperbackend.model.match

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatchBody(
    @SerialName("first_participant")
    val firstParticipant: ParticipantInMatchBody,
    @SerialName("second_participant")
    val secondParticipant: ParticipantInMatchBody,
    @SerialName("sets_to_win")
    val setsToWin: Int,
    @SerialName("regular_set_id")
    val regularSet: String? = null,
    @SerialName("deciding_set_id")
    val decidingSet: String,
)

@Serializable
data class ParticipantInMatchBody(
    @SerialName("id")
    val id: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("primary_color")
    val primaryColor: String,
    @SerialName("secondary_color")
    val secondaryColor: String?=null,
)
