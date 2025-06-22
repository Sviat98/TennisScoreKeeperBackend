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
    val player: PlayerInMatchDto,
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
    @SerialName("first_player")
    val firstPlayer: PlayerInMatchDto,
    @SerialName("second_player")
    val secondPlayer: PlayerInMatchDto,
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
        player = this.player.toPlayerInSinglesMatchDto(),
    )

fun DoublesParticipantEntity.toParticipantInMatchDto(
    displayName: String,
    primaryColor: String,
    secondaryColor: String?,
    servingParticipantId: Int?,
    servingInPairPlayerId: Int?,
    winningParticipantId: Int?,
    retiredParticipantId: Int?,
): ParticipantInMatchDto {

    val firstPlayerInBaseDto = this.firstPlayer.toPlayerInDoublesMatchDto(servingPlayerId = servingInPairPlayerId)
    val secondPlayerInBaseDto = this.secondPlayer.toPlayerInDoublesMatchDto(servingPlayerId = servingInPairPlayerId)

    val saveOrderAtDisplay = this.saveOrderAtDisplay
    return ParticipantInDoublesMatchDto(
        id = this.id.value.toString(),
        seed = this.seed,
        displayName = displayName,
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        isServing = this.id.value == servingParticipantId,
        isWinner = this.id.value == winningParticipantId,
        isRetired = this.id.value == retiredParticipantId,
        firstPlayer = if (saveOrderAtDisplay) firstPlayerInBaseDto else secondPlayerInBaseDto,
        secondPlayer = if (saveOrderAtDisplay) secondPlayerInBaseDto else firstPlayerInBaseDto,
    )
}