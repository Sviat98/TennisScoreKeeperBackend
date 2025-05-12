package com.bashkevich.tennisscorekeeperbackend.model.participant

import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerInMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.player.toDto
import com.bashkevich.tennisscorekeeperbackend.model.player.toPlayerInDoublesMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.player.toPlayerInSinglesMatchDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ParticipantDto {
    @SerialName("id")
    abstract val id: String

    @SerialName("seed")
    abstract val seed: Int?

    @SerialName("display_name")
    abstract val displayName: String

    @SerialName("is_serving")
    abstract val isServing: Boolean

    @SerialName("is_winner")
    abstract val isWinner: Boolean
}

@Serializable
@SerialName("singles_participant")
data class SinglesParticipantDto(
    @SerialName("id")
    override val id: String,
    @SerialName("seed")
    override val seed: Int? = null,
    @SerialName("display_name")
    override val displayName: String,
    @SerialName("is_serving")
    override val isServing: Boolean,
    @SerialName("is_winner")
    override val isWinner: Boolean,
    @SerialName("player")
    val player: PlayerInMatchDto,
) : ParticipantDto()


@Serializable
@SerialName("doubles_participant")
data class DoublesParticipantDto(
    @SerialName("id")
    override val id: String,
    @SerialName("seed")
    override val seed: Int? = null,
    @SerialName("display_name")
    override val displayName: String,
    @SerialName("is_serving")
    override val isServing: Boolean,
    @SerialName("is_winner")
    override val isWinner: Boolean,
    @SerialName("first_player")
    val firstPlayer: PlayerInMatchDto,
    @SerialName("second_player")
    val secondPlayer: PlayerInMatchDto,
) : ParticipantDto()

fun SinglesParticipantEntity.toDto(displayName: String, servingParticipantId: Int?, winningParticipantId: Int?): ParticipantDto =
    SinglesParticipantDto(
        id = this.id.value.toString(),
        seed = this.seed,
        displayName = displayName,
        isServing = this.id.value == servingParticipantId,
        isWinner = this.id.value == winningParticipantId,
        player = this.player.toPlayerInSinglesMatchDto(),
    )

fun DoublesParticipantEntity.toDto(
    displayName: String,
    servingParticipantId: Int?,
    servingInPairPlayerId: Int?,
    winningParticipantId: Int?,
) : ParticipantDto = DoublesParticipantDto(
    id = this.id.value.toString(),
    seed = this.seed,
    displayName = displayName,
    isServing = this.id.value == servingParticipantId,
    isWinner = this.id.value == winningParticipantId,
    firstPlayer = this.firstPlayer.toPlayerInDoublesMatchDto(servingPlayerId = servingInPairPlayerId),
    secondPlayer = this.secondPlayer.toPlayerInDoublesMatchDto(servingPlayerId = servingInPairPlayerId)
)