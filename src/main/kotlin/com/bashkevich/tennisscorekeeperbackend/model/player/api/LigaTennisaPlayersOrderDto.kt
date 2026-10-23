package com.bashkevich.tennisscorekeeperbackend.model.player.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Списки зарегистрированных игроков турнира из поля players_order ответа /api/tournaments
 * Лиги тенниса (players_order — JSON-строка внутри JSON-ответа, разбирается отдельно).
 * players — id игроков одиночного турнира, pairs — пары id игроков парного турнира
 * (пара записана в порядке регистрации: [первый, второй]).
 */
@Serializable
data class LigaTennisaPlayersOrderDto(
    @SerialName("players")
    val players: List<Int> = emptyList(),
    @SerialName("pairs")
    val pairs: List<List<Int>> = emptyList(),
)
