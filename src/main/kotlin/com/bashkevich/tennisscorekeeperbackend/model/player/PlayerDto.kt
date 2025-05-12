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
    val name: String,
)

@Serializable
data class PlayerBodyDto(
    @SerialName("surname")
    val surname: String,
    @SerialName("name")
    val name: String,
)

//@Serializable
//data class PlayerInMatchDto(
//    @SerialName("id")
//    val id: String,
//    @SerialName("surname")
//    val surname: String,
//    @SerialName("name")
//    val name: String,
//    @SerialName("is_serving")
//    val isServing: Boolean,
//    @SerialName("is_winner")
//    val isWinner: Boolean,
//)

@Serializable
sealed class PlayerInMatchDto {
    @SerialName("id")
    abstract val id: String

    @SerialName("surname")
    abstract val surname: String

    @SerialName("name")
    abstract val name: String
}

@Serializable
@SerialName("singles_player")
data class PlayerInSinglesMatchDto(
    @SerialName("id")
    override val id: String,
    @SerialName("surname")
    override val surname: String,
    @SerialName("name")
    override val name: String,
) : PlayerInMatchDto()

@Serializable
@SerialName("doubles_player")
data class PlayerInDoublesMatchDto(
    @SerialName("id")
    override val id: String,
    @SerialName("surname")
    override val surname: String,
    @SerialName("name")
    override val name: String,
    @SerialName("is_serving")
    val isServing: Boolean,
) : PlayerInMatchDto()


fun PlayerEntity.toPlayerInSinglesMatchDto(): PlayerInMatchDto = PlayerInSinglesMatchDto(
    id = this.id.value.toString(),
    surname = this.surname,
    name = this.name
)

fun PlayerEntity.toPlayerInDoublesMatchDto(servingPlayerId: Int?): PlayerInMatchDto = PlayerInDoublesMatchDto(
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
