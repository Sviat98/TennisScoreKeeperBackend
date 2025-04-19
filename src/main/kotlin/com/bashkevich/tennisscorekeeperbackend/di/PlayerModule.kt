package com.bashkevich.tennisscorekeeperbackend.di

import com.bashkevich.tennisscorekeeperbackend.feature.player.PlayerRepository
import com.bashkevich.tennisscorekeeperbackend.feature.player.PlayerService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val playerModule = module{
    singleOf(::PlayerRepository)
    singleOf(::PlayerService)
}