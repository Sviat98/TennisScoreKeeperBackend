package com.bashkevich.tennisscorekeeperbackend.plugins

import com.bashkevich.tennisscorekeeperbackend.feature.auth.AuthService
import com.bashkevich.tennisscorekeeperbackend.feature.tournament.TournamentService
import io.ktor.server.application.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

fun Application.configureScheduler(){
    val authService by inject<AuthService>()
    val tournamentService by inject<TournamentService>()

    CoroutineScope(Dispatchers.IO).launch {
        while (isActive) {
            // Ваша периодическая задача
            authService.removeAllExpiredRefreshTokens()

            // Ждем 1 день перед следующим выполнением
            delay(1.days)
        }
    }

    // Обновление кэша турниров внешнего API (Лиги тенниса): первый прогон сразу при
    // старте, затем раз в час. Ошибка одного прогона (недоступность внешнего API) не
    // завершает цикл — в кэше остаётся последний успешный список.
    CoroutineScope(Dispatchers.IO).launch {
        while (isActive) {
            runCatching { tournamentService.refreshApiTournaments() }
                .onFailure { println("Tournament api cache refresh failed: $it") }

            delay(1.hours)
        }
    }
}
