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
    @SerialName("is_retired")
    abstract val isRetired: Boolean
}

@Serializable
@SerialName("singles_participant")
data class ParticipantInShortSinglesMatchDto(
    @SerialName("id")
    override val id: String,
    @SerialName("seed")
    override val seed: Int? = null,
    @SerialName("is_winner")
    override val isWinner: Boolean,
    @SerialName("is_retired")
    override val isRetired: Boolean,
    @SerialName("player")
    val player: PlayerDto
) : ParticipantInShortMatchDto()

@Serializable
@SerialName("doubles_participant")
data class ParticipantInShortDoublesMatchDto(
    @SerialName("id")
    override val id: String,
    @SerialName("seed")
    override val seed: Int? = null,
    @SerialName("is_winner")
    override val isWinner: Boolean,
    @SerialName("is_retired")
    override val isRetired: Boolean,
    @SerialName("first_player")
    val firstPlayer: PlayerDto,
    @SerialName("second_player")
    val secondPlayer: PlayerDto,
) : ParticipantInShortMatchDto()

fun SinglesParticipantEntity.toShortMatchParticipantDto(winningParticipantId: Int?, retiredParticipantId: Int?) = ParticipantInShortSinglesMatchDto(
    id = this.id.value.toString(),
    seed = this.seed,
    isWinner = this.id.value == winningParticipantId,
    isRetired = this.id.value == retiredParticipantId,
    player = this.player.toDto(),
)

fun DoublesParticipantEntity.toShortMatchParticipantDto(winningParticipantId: Int?, retiredParticipantId: Int?) = ParticipantInShortDoublesMatchDto(
    id = this.id.value.toString(),
    seed = this.seed,
    isWinner = this.id.value == winningParticipantId,
    isRetired = this.id.value == retiredParticipantId,
    firstPlayer = this.firstPlayer.toDto(),
    secondPlayer = this.secondPlayer.toDto()
)