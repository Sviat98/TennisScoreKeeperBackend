package com.bashkevich.tennisscorekeeperbackend.feature.tournament

import com.bashkevich.tennisscorekeeperbackend.model.tournament.LigaTennisaTournamentDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * Клиент внешнего API Лиги тенниса. Базовый URL и JSON (ignoreUnknownKeys = true)
 * настроены на HttpClient в di/LigaTennisaModule.
 */
class LigaTennisaApiClient(private val client: HttpClient) {

    suspend fun getTournaments(): List<LigaTennisaTournamentDto> = client.get("tournaments").body()
}
