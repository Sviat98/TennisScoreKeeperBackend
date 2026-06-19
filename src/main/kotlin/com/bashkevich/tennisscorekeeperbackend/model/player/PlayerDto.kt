package com.bashkevich.tennisscorekeeperbackend.model.player

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerDto(
    @SerialName("id")
    val id: String,
    @SerialName("surname")
    val surname: String,
    @SerialName("name")
    val name: String,
    @SerialName("date_birth")
    val dateBirth: LocalDate,
)

@Serializable
data class PlayerBodyDto(
    @SerialName("surname")
    val surname: String,
    @SerialName("name")
    val name: String,
    @SerialName("date_birth")
    val dateBirth: LocalDate,
)

fun PlayerEntity.toDto() = PlayerDto(
    id = this.id.value.toString(),
    surname = this.surname,
    name = this.name,
    dateBirth = this.dateBirth
)
