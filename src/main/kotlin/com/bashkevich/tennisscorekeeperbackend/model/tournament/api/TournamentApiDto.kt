package com.bashkevich.tennisscorekeeperbackend.model.tournament.api

import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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