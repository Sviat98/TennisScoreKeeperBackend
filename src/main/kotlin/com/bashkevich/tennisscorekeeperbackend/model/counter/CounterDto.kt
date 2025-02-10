package com.bashkevich.tennisscorekeeperbackend.model.counter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CounterDto(
    @SerialName(value = "id")
    val id: String,
    @SerialName(value = "name")
    val name: String,
    @SerialName(value = "value")
    val value: Int,
)

@Serializable
data class CounterBodyDto(
    @SerialName(value = "name")
    val name: String,
    @SerialName(value = "value")
    val value: Int = 0,
)

@Serializable
data class CounterDeltaDto(
    @SerialName(value = "delta")
    val delta: Int,
)

fun CounterEntity.toDto() = CounterDto(
    id = this.id.value.toString(),
    name = this.name,
    value = this.value
)