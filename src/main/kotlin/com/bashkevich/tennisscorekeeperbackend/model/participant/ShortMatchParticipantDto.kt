package com.bashkevich.tennisscorekeeperbackend.model.participant

import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerDto
import com.bashkevich.tennisscorekeeperbackend.model.player.toDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ParticipantInShortMatchDto{
    @SerialName("id")
    abstract val id: String
    @SerialName("seed")
    abstract val seed: Int?
    @SerialName("is_winner")
    abstract val isWinner: Boolean
}

@Serializable
@SerialName("singles_participant")
data class ShortMatchSinglesParticipantDto(
    @SerialName("id")
    override val id: String,
    @SerialName("seed")
    override val seed: Int? = null,
    @SerialName("is_winner")
    override val isWinner: Boolean,
    @SerialName("player")
    val player: PlayerDto
) : ParticipantInShortMatchDto()

@Serializable
@SerialName("doubles_participant")
data class ShortMatchDoublesParticipantDto(
    @SerialName("id")
    override val id: String,
    @SerialName("seed")
    override val seed: Int? = null,
    @SerialName("is_winner")
    override val isWinner: Boolean,
    @SerialName("first_player")
    val firstPlayer: PlayerDto,
    @SerialName("second_player")
    val secondPlayer: PlayerDto,
) : ParticipantInShortMatchDto()

fun SinglesParticipantEntity.toShortMatchParticipantDto(winningParticipantId: Int?) = ShortMatchSinglesParticipantDto(
    id = this.id.value.toString(),
    seed = this.seed,
    isWinner = this.id.value == winningParticipantId,
    player = this.player.toDto(),
)

fun DoublesParticipantEntity.toShortMatchParticipantDto(winningParticipantId: Int?) = ShortMatchDoublesParticipantDto(
    id = this.id.value.toString(),
    seed = this.seed,
    isWinner = this.id.value == winningParticipantId,
    firstPlayer = this.firstPlayer.toDto(),
    secondPlayer = this.secondPlayer.toDto()
)