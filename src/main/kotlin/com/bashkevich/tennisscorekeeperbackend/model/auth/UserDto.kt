package com.bashkevich.tennisscorekeeperbackend.model.auth

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("id")
    val id: String,
    @SerialName("surname")
    val surname: String,
    @SerialName("name")
    val name: String,
    @SerialName("date_birth")
    val dateBirth: LocalDate,
)

fun UserEntity.toDto() = UserDto(
    id = this.id.value.toString(),
    surname = this.surname,
    name = this.name,
    dateBirth = this.dateBirth
)
