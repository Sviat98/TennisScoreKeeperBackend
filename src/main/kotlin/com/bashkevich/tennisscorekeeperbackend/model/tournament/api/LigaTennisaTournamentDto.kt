package com.bashkevich.tennisscorekeeperbackend.model.tournament.api

import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Код tournament_type во внешнем API Лиги тенниса: одиночный турнир. */
const val LT_TOURNAMENT_TYPE_SINGLES = 200

/** Код tournament_type во внешнем API Лиги тенниса: парный турнир. */
const val LT_TOURNAMENT_TYPE_DOUBLES = 201

/** Код status во внешнем API Лиги тенниса: турнир ещё не начался, запись открыта. */
const val LT_TOURNAMENT_STATUS_NOT_STARTED = 1

/** Код status во внешнем API Лиги тенниса: турнир идёт. */
const val LT_TOURNAMENT_STATUS_IN_PROGRESS = 2

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
    /**
     * Зарегистрированные игроки турнира — JSON-строка вида
     * {"players":[id, ...],"pairs":[[id, id], ...]}. Null, пока на турнир в Лиге тенниса
     * никто не зарегистрировался. Для одиночного турнира берётся players, для парного — pairs.
     */
    @SerialName("players_order")
    val playersOrder: String? = null,
)

