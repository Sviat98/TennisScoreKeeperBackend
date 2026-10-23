package com.bashkevich.tennisscorekeeperbackend.feature.tournament.api

import com.bashkevich.tennisscorekeeperbackend.model.player.api.LigaTennisaPlayerDto
import com.bashkevich.tennisscorekeeperbackend.model.tournament.api.LigaTennisaTournamentDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * Клиент внешнего API Лиги тенниса. Базовый URL и JSON (ignoreUnknownKeys = true)
 * настроены на HttpClient в di/LigaTennisaModule.
 */
class LigaTennisaApiClient(private val client: HttpClient) {

    suspend fun getTournaments(): List<LigaTennisaTournamentDto> = client.get("tournaments").body()

    /** Все игроки внешнего API (для предзаполнения связей player_api_relation). */
    suspend fun getAllPlayers(): List<LigaTennisaPlayerDto> = client.get("players").body()

    /** Один игрок внешнего API по его id. */
    suspend fun getPlayerById(playerId: Int): LigaTennisaPlayerDto = client.get("players/$playerId").body()
}
