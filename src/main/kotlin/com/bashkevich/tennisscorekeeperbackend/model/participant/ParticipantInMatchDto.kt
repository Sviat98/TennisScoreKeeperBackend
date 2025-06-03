package com.bashkevich.tennisscorekeeperbackend.model.participant

import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerInMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.player.toPlayerInDoublesMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.player.toPlayerInSinglesMatchDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ParticipantInMatchDto {
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
data class SinglesParticipantInMatchDto(
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
) : ParticipantInMatchDto()


@Serializable
@SerialName("doubles_participant")
data class DoublesParticipantInMatchDto(
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
) : ParticipantInMatchDto()

fun SinglesParticipantEntity.toParticipantInMatchDto(displayName: String, servingParticipantId: Int?, winningParticipantId: Int?): ParticipantInMatchDto =
    SinglesParticipantInMatchDto(
        id = this.id.value.toString(),
        seed = this.seed,
        displayName = displayName,
        isServing = this.id.value == servingParticipantId,
        isWinner = this.id.value == winningParticipantId,
        player = this.player.toPlayerInSinglesMatchDto(),
    )

fun DoublesParticipantEntity.toParticipantInMatchDto(
    displayName: String,
    servingParticipantId: Int?,
    servingInPairPlayerId: Int?,
    winningParticipantId: Int?,
) : ParticipantInMatchDto {

    val firstPlayerInBaseDto = this.firstPlayer.toPlayerInDoublesMatchDto(servingPlayerId = servingInPairPlayerId)
    val secondPlayerInBaseDto = this.secondPlayer.toPlayerInDoublesMatchDto(servingPlayerId = servingInPairPlayerId)

    val saveOrderAtDisplay = this.saveOrderAtDisplay
    return DoublesParticipantInMatchDto(
        id = this.id.value.toString(),
        seed = this.seed,
        displayName = displayName,
        isServing = this.id.value == servingParticipantId,
        isWinner = this.id.value == winningParticipantId,
        firstPlayer = if (saveOrderAtDisplay) firstPlayerInBaseDto else secondPlayerInBaseDto,
        secondPlayer = if (saveOrderAtDisplay) secondPlayerInBaseDto else firstPlayerInBaseDto,
    )
}