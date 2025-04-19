package com.bashkevich.tennisscorekeeperbackend.di

import com.bashkevich.tennisscorekeeperbackend.feature.match.MatchLogRepository
import com.bashkevich.tennisscorekeeperbackend.feature.match.MatchRepository
import com.bashkevich.tennisscorekeeperbackend.feature.match.MatchService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val matchModule = module {
    singleOf(::MatchRepository)
    singleOf(::MatchLogRepository)
    singleOf(::MatchService)
}