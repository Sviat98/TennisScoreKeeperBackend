package com.bashkevich.tennisscorekeeperbackend.model.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ThemeDto(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("content")
    val content: ThemeContent,
)

fun ThemeEntity.toDto() = ThemeDto(
    id = id.value.toString(),
    name = name,
    content = content
)
