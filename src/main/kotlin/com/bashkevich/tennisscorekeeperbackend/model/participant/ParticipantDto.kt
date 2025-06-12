package com.bashkevich.tennisscorekeeperbackend.model.participant

import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerInParticipantDto
import com.bashkevich.tennisscorekeeperbackend.model.player.toPlayerInParticipantDto
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
    val player: PlayerInParticipantDto,
) : ParticipantDto()

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
    val secondPlayer: PlayerInParticipantDto,
) : ParticipantDto()

fun SinglesParticipantEntity.toDto(): ParticipantDto =
    SinglesParticipantDto(id = this.id.toString(), seed = this.seed, player = this.player.toPlayerInParticipantDto())

fun DoublesParticipantEntity.toDto(): ParticipantDto {
    val saveOrderAtDisplay = this.saveOrderAtDisplay

    val firstPlayerInBaseDto = this.firstPlayer.toPlayerInParticipantDto()
    val secondPlayerInBaseDto = this.secondPlayer.toPlayerInParticipantDto()

    return DoublesParticipantDto(
        id = this.id.toString(),
        seed = this.seed,
        firstPlayer = if (saveOrderAtDisplay) firstPlayerInBaseDto else secondPlayerInBaseDto,
        secondPlayer = if (saveOrderAtDisplay) secondPlayerInBaseDto else firstPlayerInBaseDto,
    )
}