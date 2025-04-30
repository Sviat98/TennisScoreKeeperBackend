package com.bashkevich.tennisscorekeeperbackend.model.match

data class ShortMatchEntity (
    val id: Int,
    val firstPlayer: PlayerInShortMatchEntity,
    val secondPlayer: PlayerInShortMatchEntity,
    val status: MatchStatus,
    val winnerPlayerId: Int?
)

data class PlayerInShortMatchEntity(
    val id: Int,
    val surname: String,
    val name: String,
)
