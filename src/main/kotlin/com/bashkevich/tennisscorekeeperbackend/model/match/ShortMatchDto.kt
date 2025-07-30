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
    @SerialName("previous_sets")
    val previousSets: List<TennisSetDto>,
    @SerialName("current_set")
    val currentSet: TennisSetDto?,
    @SerialName("current_game")
    val currentGame: TennisGameDto?,
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

fun DoublesMatchEntity.toShortMatchDto(
    previousSets: List<TennisSetDto> = emptyList(),
    currentSet: TennisSetDto? = null,
    currentGame: TennisGameDto? = null,
) = ShortMatchDto(
    id = this.id.toString(),
    firstParticipant = this.firstParticipant.toShortMatchParticipantDto(
        winningParticipantId = this.winnerParticipant?.id?.value,
        retiredParticipantId = this.retiredParticipant?.id?.value
    ),
    secondParticipant = this.secondParticipant.toShortMatchParticipantDto(
        winningParticipantId = this.winnerParticipant?.id?.value,
        retiredParticipantId = this.retiredParticipant?.id?.value
    ),
    status = this.status,
    previousSets = previousSets,
    currentSet = currentSet,
    currentGame = currentGame
)

fun SinglesMatchEntity.toShortMatchDto(
    previousSets: List<TennisSetDto> = emptyList(),
    currentSet: TennisSetDto? = null,
    currentGame: TennisGameDto? = null,
) = ShortMatchDto(
    id = this.id.toString(),
    firstParticipant = this.firstParticipant.toShortMatchParticipantDto(
        winningParticipantId = this.winnerParticipant?.id?.value,
        retiredParticipantId = this.retiredParticipant?.id?.value
    ),
    secondParticipant = this.secondParticipant.toShortMatchParticipantDto(
        winningParticipantId = this.winnerParticipant?.id?.value,
        retiredParticipantId = this.retiredParticipant?.id?.value
    ),
    status = this.status,
    previousSets = previousSets,
    currentSet = currentSet,
    currentGame = currentGame
)

//fun PlayerInShortMatchEntity.toDto(winnerPlayerId: Int?) = PlayerInShortMatchDto(
//    id = this.id.toString(),
//    surname = this.surname,
//    name = this.name,
//    winner = this.id == winnerPlayerId
//)
