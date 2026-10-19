package com.bashkevich.tennisscorekeeperbackend.di

import com.bashkevich.tennisscorekeeperbackend.feature.tournament.api.LigaTennisaApiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * HttpClient и клиент внешнего API Лиги тенниса. Вынесен в отдельный модуль,
 * потому что клиент будет использоваться не только в турнирной фиче.
 */
val ligaTennisaModule = module {
    single<HttpClient> {
        HttpClient(CIO) {
            expectSuccess = true

            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
            }

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }

            install(DefaultRequest) {
                val baseUrl = System.getenv("LT_API_BASE_URL") ?: error("You must specify LT API Base URL")
                // Без завершающего "/" DefaultRequest теряет последний сегмент пути базы
                // (/api) и запрос уходит на /tournaments вместо /api/tournaments
                url(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            }
        }
    }

    singleOf(::LigaTennisaApiClient)
}
