package com.bashkevich.tennisscorekeeperbackend.model.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResponseMessageDto(
    @SerialName(value = "message")
    val message: String
)
