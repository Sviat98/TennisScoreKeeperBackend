package com.bashkevich.tennisscorekeeperbackend.plugins

import com.bashkevich.tennisscorekeeperbackend.di.counterModule
import com.bashkevich.tennisscorekeeperbackend.di.matchModule
import com.bashkevich.tennisscorekeeperbackend.di.participantModule
import com.bashkevich.tennisscorekeeperbackend.di.playerModule
import com.bashkevich.tennisscorekeeperbackend.di.setTemplateModule
import com.bashkevich.tennisscorekeeperbackend.di.tournamentModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDependencyInjection() {
    install(Koin) {
        slf4jLogger()
        modules(counterModule, playerModule, matchModule, setTemplateModule, tournamentModule, participantModule)
    }
}
