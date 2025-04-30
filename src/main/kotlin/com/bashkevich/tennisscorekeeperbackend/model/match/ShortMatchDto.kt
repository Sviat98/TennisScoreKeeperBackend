package com.bashkevich.tennisscorekeeperbackend.model.match

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShortMatchDto(
    @SerialName("id")
    val id: String,
    @SerialName("first_player")
    val firstPlayer: PlayerInShortMatchDto,
    @SerialName("second_player")
    val secondPlayer: PlayerInShortMatchDto,
    val status: MatchStatus,
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

fun ShortMatchEntity.toDto() = ShortMatchDto(
    id = this.id.toString(),
    firstPlayer = this.firstPlayer.toDto(winnerPlayerId = this.winnerPlayerId),
    secondPlayer = this.firstPlayer.toDto(winnerPlayerId = this.winnerPlayerId),
    status = this.status
)

fun PlayerInShortMatchEntity.toDto(winnerPlayerId: Int?) = PlayerInShortMatchDto(
    id = this.id.toString(),
    surname = this.surname,
    name = this.name,
    winner = this.id == winnerPlayerId
)
