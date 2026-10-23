package com.bashkevich.tennisscorekeeperbackend.model.player.api

import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Дата рождения, вставляемая игроку, если в API Лиги тенниса она битая или не указана вовсе. */
val LT_FALLBACK_DATE_BIRTH = LocalDate(1980, 1, 1)

/**
 * Игрок из внешнего API Лиги тенниса (ligatennisa.com/api/players).
 * Поля, кроме id, защитно nullable: у игрока без одного из них должен отфильтроваться
 * только сам игрок, а не упасть разбор всего списка.
 * Неиспользуемые поля ответа отбрасываются настройкой ignoreUnknownKeys.
 */
@Serializable
data class LigaTennisaPlayerDto(
    @SerialName("id")
    val id: Int,
    @SerialName("first_name")
    val firstName: String? = null,
    @SerialName("last_name")
    val lastName: String? = null,
    @SerialName("date_of_birth")
    val dateOfBirth: String? = null,
    /** Эло для одиночных турниров. */
    @SerialName("elo_points")
    val eloPoints: Int? = null,
    /** Эло для парных турниров. */
    @SerialName("doubles_elo_points")
    val doublesEloPoints: Int? = null,
)

/**
 * Дата рождения игрока: отсутствующая или битая (например, .NET-значение по умолчанию вида
 * 0001-06-04T00:00:00.000Z) заменяется на LT_FALLBACK_DATE_BIRTH.
 */
fun LigaTennisaPlayerDto.resolveDateBirth(): LocalDate {
    val parsed = dateOfBirth?.let { raw ->
        runCatching { Instant.parse(raw).toLocalDateTime(TimeZone.UTC).date }.getOrNull()
    }

    return if (parsed == null || parsed.year < 1900) LT_FALLBACK_DATE_BIRTH else parsed
}
