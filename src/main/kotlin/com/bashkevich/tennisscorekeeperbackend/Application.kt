package com.bashkevich.tennisscorekeeperbackend

import com.bashkevich.tennisscorekeeperbackend.plugins.configureCors
import com.bashkevich.tennisscorekeeperbackend.plugins.configureDatabase
import com.bashkevich.tennisscorekeeperbackend.plugins.configureDependencyInjection
import com.bashkevich.tennisscorekeeperbackend.plugins.configureMonitoring
import com.bashkevich.tennisscorekeeperbackend.plugins.configureRouting
import com.bashkevich.tennisscorekeeperbackend.plugins.configureSerialization
import com.bashkevich.tennisscorekeeperbackend.plugins.configureSockets
import com.bashkevich.tennisscorekeeperbackend.plugins.configureStatusPages
import com.bashkevich.tennisscorekeeperbackend.plugins.configureValidation
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) {
    EngineMain.main(args)
}

suspend fun Application.module() {
    configureSerialization()
    configureCors()
    configureSockets()
    configureDependencyInjection()
    configureMonitoring()
    configureDatabase()
    configureRouting()
    configureStatusPages()
    configureValidation()
}
