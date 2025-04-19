package com.bashkevich.tennisscorekeeperbackend.model.player

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerDto(
    @SerialName("id")
    val id: String,
    @SerialName("surname")
    val surname: String,
    @SerialName("name")
    val name: String
)

@Serializable
data class PlayerBodyDto(
    @SerialName("surname")
    val surname: String,
    @SerialName("name")
    val name: String
)

@Serializable
data class PlayerInMatchDto(
    @SerialName("id")
    val id: String,
    @SerialName("surname")
    val surname: String,
    @SerialName("name")
    val name: String,
    @SerialName("is_serving")
    val isServing: Boolean
)

fun PlayerEntity.toPlayerInMatchDto(servingPlayerId: Int?) = PlayerInMatchDto(
    id = this.id.value.toString(),
    surname = this.surname,
    name = this.name,
    isServing = this.id.value == servingPlayerId
)

fun PlayerEntity.toDto() = PlayerDto(
    id = this.id.value.toString(),
    surname = this.surname,
    name = this.name
)
