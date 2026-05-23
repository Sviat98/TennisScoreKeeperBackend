package com.bashkevich.tennisscorekeeperbackend.model.theme

import kotlinx.serialization.Serializable

@Serializable
data class ThemeColor(
    val color: String,
    val alpha: Float = 1f,
)

@Serializable
data class ThemeContent(
    val backgroundColor: ThemeColor,
    val textColor: ThemeColor,
)
