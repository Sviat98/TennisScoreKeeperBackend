package com.bashkevich.tennisscorekeeperbackend.model.participant

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ParticipantDto {
    @SerialName("id")
    abstract val id: String

    @SerialName("seed")
    abstract val seed: Int?
}

@Serializable
@SerialName("singles_participant")
data class SinglesParticipantDto(
    @SerialName("id")
    override val id: String,
    @SerialName("seed")
    override val seed: Int?,
    @SerialName("player")
    val player: PlayerInParticipantDto
): ParticipantDto()

@Serializable
@SerialName("doubles_participant")
data class DoublesParticipantDto(
    @SerialName("id")
    override val id: String,
    @SerialName("seed")
    override val seed: Int?,
    @SerialName("first_player")
    val firstPlayer: PlayerInParticipantDto,
    @SerialName("second_player")
    val secondPlayer: PlayerInParticipantDto
): ParticipantDto()

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