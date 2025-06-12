package com.bashkevich.tennisscorekeeperbackend.model.player

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerInParticipantDto(
    @SerialName("id")
    val id: String,
    @SerialName("surname")
    val surname: String,
    @SerialName("name")
    val name: String,
)

fun PlayerEntity.toPlayerInParticipantDto() = PlayerInParticipantDto(
    id = this.id.value.toString(),
    surname = this.surname,
    name = this.name
)