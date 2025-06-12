package com.bashkevich.tennisscorekeeperbackend.model.match

import com.bashkevich.tennisscorekeeperbackend.model.match.doubles.DoublesMatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.match.singles.SinglesMatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.participant.ParticipantInShortMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.participant.toShortMatchParticipantDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShortMatchDto(
    @SerialName("id")
    val id: String,
    @SerialName("first_participant")
    val firstParticipant: ParticipantInShortMatchDto,
    @SerialName("second_participant")
    val secondParticipant: ParticipantInShortMatchDto,
    @SerialName("status")
    val status: MatchStatus,
    @SerialName("final_score")
    val finalScore: List<TennisSetDto>,
)

@Serializable
data class PlayerInShortMatchDto(
    @SerialName("id")
    val id: String,
    @SerialName("surname")
    val surname: String,
    @SerialName("name")
    val name: String,
    @SerialName("winner")
    val winner: Boolean,
)

fun DoublesMatchEntity.toShortMatchDto(finalScore: List<TennisSetDto> = emptyList()) = ShortMatchDto(
    id = this.id.toString(),
    firstParticipant = this.firstParticipant.toShortMatchParticipantDto(winningParticipantId = this.winner?.id?.value),
    secondParticipant = this.secondParticipant.toShortMatchParticipantDto(winningParticipantId = this.winner?.id?.value),
    status = this.status,
    finalScore = finalScore
)

fun SinglesMatchEntity.toShortMatchDto(finalScore: List<TennisSetDto> = emptyList()) = ShortMatchDto(
    id = this.id.toString(),
    firstParticipant = this.firstParticipant.toShortMatchParticipantDto(winningParticipantId = this.winner?.id?.value),
    secondParticipant = this.secondParticipant.toShortMatchParticipantDto(winningParticipantId = this.winner?.id?.value),
    status = this.status,
    finalScore = finalScore
)

//fun PlayerInShortMatchEntity.toDto(winnerPlayerId: Int?) = PlayerInShortMatchDto(
//    id = this.id.toString(),
//    surname = this.surname,
//    name = this.name,
//    winner = this.id == winnerPlayerId
//)
