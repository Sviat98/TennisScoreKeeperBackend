package com.bashkevich.tennisscorekeeperbackend.model.tournament

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Код tournament_type во внешнем API Лиги тенниса: одиночный турнир. */
const val LT_TOURNAMENT_TYPE_SINGLES = 200

/** Код tournament_type во внешнем API Лиги тенниса: парный турнир. */
const val LT_TOURNAMENT_TYPE_DOUBLES = 201

/** Код status во внешнем API Лиги тенниса: турнир ещё не начался, запись открыта. */
const val LT_TOURNAMENT_STATUS_ACTIVE = 1

/**
 * Запись турнира из внешнего API Лиги тенниса (ligatennisa.com/api/tournaments).
 * Поля, нужные интеграции, защитно nullable: у записи без одного из них должен отфильтроваться
 * только сам турнир (при маппинге в TournamentApiDto), а не упасть разбор всего списка.
 * Неиспользуемые поля ответа отбрасываются настройкой ignoreUnknownKeys.
 */
@Serializable
data class LigaTennisaTournamentDto(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("start_date")
    val startDate: String? = null,
    @SerialName("tournament_type")
    val tournamentType: Int? = null,
    @SerialName("status")
    val status: Int? = null,
)

/**
 * Турнир внешнего API в ответе GET /tournaments/active.
 */
@Serializable
data class TournamentApiDto(
    @SerialName("api_id")
    val apiId: String,
    @SerialName("name")
    val name: String,
    @SerialName("start_date")
    val startDate: LocalDate,
    @SerialName("type")
    val type: TournamentType,
)
