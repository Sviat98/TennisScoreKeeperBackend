package com.bashkevich.tennisscorekeeperbackend.model.match

import com.bashkevich.tennisscorekeeperbackend.model.match_log.doubles.DoublesMatchLogEvent
import com.bashkevich.tennisscorekeeperbackend.model.match_log.singles.SinglesMatchLogEvent
import com.bashkevich.tennisscorekeeperbackend.model.participant.ParticipantInMatchDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatchDto(
    @SerialName("id")
    val id: String,
    @SerialName("point_shift")
    val pointShift: Int,
    @SerialName("first_participant")
    val firstParticipant: ParticipantInMatchDto,
    @SerialName("second_participant")
    val secondParticipant: ParticipantInMatchDto,
    @SerialName("previous_sets")
    val previousSets: List<TennisSetDto>,
    @SerialName("current_set")
    val currentSet: TennisSetDto,
    @SerialName("current_game")
    val currentGame: TennisGameDto,
)

@Serializable
data class TennisSetDto(
    @SerialName("first_participant_games")
    val firstParticipantGames: Int,
    @SerialName("second_participant_games")
    val secondParticipantGames: Int,
    @SerialName("special_set_mode")
    val specialSetMode: SpecialSetMode? = null,
)

// SUPER_TIEBREAK - помечаем, что это супер-тайбрейк (на клиенте нужно залочить кнопки GAME)
// ENDLESS - там, где gamesToWin > 10, на клиенте должна быть возможность завершить досрочно
enum class SpecialSetMode {
    SUPER_TIEBREAK, ENDLESS
}

@Serializable
data class TennisGameDto(
    @SerialName("first_participant_points")
    val firstParticipantPoints: String,
    @SerialName("second_participant_points")
    val secondParticipantPoints: String,
)

fun SinglesMatchLogEvent.toTennisSetDto(specialSetMode: SpecialSetMode? = null): TennisSetDto {

    return TennisSetDto(
        firstParticipantGames = this.firstParticipantPoints,
        secondParticipantGames = this.secondParticipantPoints,
        specialSetMode = specialSetMode
    )
}

fun DoublesMatchLogEvent.toTennisSetDto(specialSetMode: SpecialSetMode? = null): TennisSetDto {

    return TennisSetDto(
        firstParticipantGames = this.firstParticipantPoints,
        secondParticipantGames = this.secondParticipantPoints,
        specialSetMode = specialSetMode
    )
}

fun SinglesMatchLogEvent.toTennisGameDto(): TennisGameDto {
    val firstPlayerScore =
        if (this.scoreType == ScoreType.TIEBREAK_POINT) this.firstParticipantPoints.toString() else mapGameScore(
            this.firstParticipantPoints,
            this.secondParticipantPoints
        )

    val secondPlayerScore =
        if (this.scoreType == ScoreType.TIEBREAK_POINT) this.secondParticipantPoints.toString() else mapGameScore(
            this.secondParticipantPoints,
            this.firstParticipantPoints
        )

    return TennisGameDto(firstParticipantPoints = firstPlayerScore, secondParticipantPoints = secondPlayerScore)
}

fun DoublesMatchLogEvent.toTennisGameDto(): TennisGameDto {
    val firstParticipantScore =
        if (this.scoreType == ScoreType.TIEBREAK_POINT) this.firstParticipantPoints.toString() else mapGameScore(
            this.firstParticipantPoints,
            this.secondParticipantPoints
        )

    val secondParticipantScore =
        if (this.scoreType == ScoreType.TIEBREAK_POINT) this.secondParticipantPoints.toString() else mapGameScore(
            this.secondParticipantPoints,
            this.firstParticipantPoints
        )

    return TennisGameDto(firstParticipantPoints = firstParticipantScore, secondParticipantPoints = secondParticipantScore)
}

fun mapGameScore(participantPoints: Int, opponentPoints: Int): String {
    return when {
        participantPoints == 0 -> "0"
        participantPoints == 1 -> "15"
        participantPoints == 2 -> "30"
        participantPoints == 3 && participantPoints > opponentPoints -> "40"
        participantPoints >= 3 && participantPoints == opponentPoints -> "40"
        participantPoints > 3 && participantPoints > opponentPoints -> "AD"
        participantPoints < opponentPoints -> ""
        else -> ""
    }
}
