package com.bashkevich.tennisscorekeeperbackend.model.participant

import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerDto
import com.bashkevich.tennisscorekeeperbackend.model.player.toDto
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

    @SerialName("primary_color")
    abstract val primaryColor: String

    @SerialName("secondary_color")
    abstract val secondaryColor: String?

    @SerialName("is_serving")
    abstract val isServing: Boolean

    @SerialName("is_winner")
    abstract val isWinner: Boolean

    @SerialName("is_retired")
    abstract val isRetired: Boolean
}

@Serializable
@SerialName("singles_participant")
data class ParticipantInSinglesMatchDto(
    @SerialName("id")
    override val id: String,
    @SerialName("seed")
    override val seed: Int?,
    @SerialName("display_name")
    override val displayName: String,
    @SerialName("primary_color")
    override val primaryColor: String,
    @SerialName("secondary_color")
    override val secondaryColor: String?,
    @SerialName("is_serving")
    override val isServing: Boolean,
    @SerialName("is_winner")
    override val isWinner: Boolean,
    @SerialName("is_retired")
    override val isRetired: Boolean,
    @SerialName("player")
    val player: PlayerDto,
) : ParticipantInMatchDto()


@Serializable
@SerialName("doubles_participant")
data class ParticipantInDoublesMatchDto(
    @SerialName("id")
    override val id: String,
    @SerialName("seed")
    override val seed: Int?,
    @SerialName("display_name")
    override val displayName: String,
    @SerialName("primary_color")
    override val primaryColor: String,
    @SerialName("secondary_color")
    override val secondaryColor: String?,
    @SerialName("is_serving")
    override val isServing: Boolean,
    @SerialName("is_winner")
    override val isWinner: Boolean,
    @SerialName("is_retired")
    override val isRetired: Boolean,
    @SerialName("serving_player_id")
    val servingPlayerId: String?,
    @SerialName("first_player")
    val firstPlayer: PlayerDto,
    @SerialName("second_player")
    val secondPlayer: PlayerDto,
) : ParticipantInMatchDto()

fun SinglesParticipantEntity.toParticipantInMatchDto(
    displayName: String,
    primaryColor: String,
    secondaryColor: String?,
    servingParticipantId: Int?,
    winningParticipantId: Int?,
    retiredParticipantId: Int?,
): ParticipantInMatchDto =
    ParticipantInSinglesMatchDto(
        id = this.id.value.toString(),
        seed = this.seed,
        displayName = displayName,
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        isServing = this.id.value == servingParticipantId,
        isWinner = this.id.value == winningParticipantId,
        isRetired = this.id.value == retiredParticipantId,
        player = this.player.toDto(),
    )

fun DoublesParticipantEntity.toParticipantInMatchDto(
    displayName: String,
    primaryColor: String,
    secondaryColor: String?,
    servingParticipantId: Int?,
    winningParticipantId: Int?,
    retiredParticipantId: Int?,
    nowServingPlayerId: Int?,
    nextServingPlayerId: Int?,
): ParticipantInMatchDto {

    val isServing = this.id.value == servingParticipantId
    val servingPlayerId = if (isServing) nowServingPlayerId?.toString() else nextServingPlayerId?.toString()

    val firstPlayerInBaseDto = this.firstPlayer.toDto()
    val secondPlayerInBaseDto = this.secondPlayer.toDto()

    val saveOrderAtDisplay = this.saveOrderAtDisplay
    return ParticipantInDoublesMatchDto(
        id = this.id.value.toString(),
        seed = this.seed,
        displayName = displayName,
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        isServing = isServing,
        isWinner = this.id.value == winningParticipantId,
        isRetired = this.id.value == retiredParticipantId,
        servingPlayerId = servingPlayerId,
        firstPlayer = if (saveOrderAtDisplay) firstPlayerInBaseDto else secondPlayerInBaseDto,
        secondPlayer = if (saveOrderAtDisplay) secondPlayerInBaseDto else firstPlayerInBaseDto,
    )
}