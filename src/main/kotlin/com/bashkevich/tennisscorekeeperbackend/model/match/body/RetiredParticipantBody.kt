package com.bashkevich.tennisscorekeeperbackend.model.match.body

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RetiredParticipantBody(
    @SerialName("retired_participant_id")
    val retiredParticipantId: String
)
