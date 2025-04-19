package com.bashkevich.tennisscorekeeperbackend.model.match

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerInMatchDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatchDto(
    @SerialName("id")
    val id: Int,
    @SerialName("first_player")
    val firstPlayer: PlayerInMatchDto,
    @SerialName("second_player")
    val secondPlayer: PlayerInMatchDto,
    @SerialName("previous_sets")
    val previousSets: List<TennisSetDto>,
    @SerialName("current_set")
    val currentSet: TennisSetDto,
    @SerialName("current_game")
    val currentGame: TennisGameDto,
)

@Serializable
data class TennisSetDto(
    @SerialName("first_player_games")
    val firstPlayerGames: Int,
    @SerialName("second_player_games")
    val secondPlayerGames: Int,
)

@Serializable
data class TennisGameDto(
    @SerialName("first_player_points")
    val firstPlayerPoints: String,
    @SerialName("second_player_points")
    val secondPlayerPoints: String,
)

fun MatchLogEvent.toTennisSetDto(): TennisSetDto {

    return TennisSetDto(firstPlayerGames = this.firstPlayerPoints, secondPlayerGames = this.secondPlayerPoints)
}

fun MatchLogEvent.toTennisGameDto(): TennisGameDto {
    val firstPlayerScore = mapGameScore(this.firstPlayerPoints, this.secondPlayerPoints)

    val secondPlayerScore = mapGameScore(this.secondPlayerPoints, this.firstPlayerPoints)

    return TennisGameDto(firstPlayerPoints = firstPlayerScore, secondPlayerPoints = secondPlayerScore)
}

fun mapGameScore(playerPoints: Int, opponentPoints: Int): String {
    return when {
        playerPoints == 0 -> "0"
        playerPoints == 1 -> "15"
        playerPoints == 2 -> "30"
        playerPoints == 3 && playerPoints > opponentPoints -> "40"
        playerPoints >= 3 && playerPoints == opponentPoints -> "40"
        playerPoints > 3 && playerPoints > opponentPoints -> "AD"
        playerPoints < opponentPoints -> ""
        else -> ""
    }
}
