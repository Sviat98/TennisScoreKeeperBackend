package com.bashkevich.tennisscorekeeperbackend.plugins

import com.bashkevich.tennisscorekeeperbackend.feature.auth.AuthService
import io.ktor.server.application.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject
import java.util.concurrent.TimeUnit

fun Application.configureScheduler(){
    val authService by inject<AuthService>()

    CoroutineScope(Dispatchers.IO).launch {
        while (isActive) {
            // Ваша периодическая задача
            authService.removeAllExpiredRefreshTokens()

            // Ждем 1 день перед следующим выполнением
            delay(TimeUnit.DAYS.toMillis(1))
        }
    }
}