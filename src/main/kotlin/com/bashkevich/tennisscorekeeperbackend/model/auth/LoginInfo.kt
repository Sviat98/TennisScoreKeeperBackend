package com.bashkevich.tennisscorekeeperbackend.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginInfo(
    @SerialName("login")
    val login: String,
    @SerialName("password")
    val password: String
)
