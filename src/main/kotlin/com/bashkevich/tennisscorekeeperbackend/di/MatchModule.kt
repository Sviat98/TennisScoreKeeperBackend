package com.bashkevich.tennisscorekeeperbackend.di

import com.bashkevich.tennisscorekeeperbackend.feature.match.match_log.MatchLogRepository
import com.bashkevich.tennisscorekeeperbackend.feature.match.MatchRepository
import com.bashkevich.tennisscorekeeperbackend.feature.match.MatchService
import com.bashkevich.tennisscorekeeperbackend.feature.match.set_template.SetTemplateRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val matchModule = module {
    singleOf(::MatchRepository)
    singleOf(::MatchLogRepository)
    singleOf(::SetTemplateRepository)
    singleOf(::MatchService)
}