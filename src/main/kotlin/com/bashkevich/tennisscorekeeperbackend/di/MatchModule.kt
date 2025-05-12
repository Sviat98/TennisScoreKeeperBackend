package com.bashkevich.tennisscorekeeperbackend.di

import com.bashkevich.tennisscorekeeperbackend.feature.match.MatchServiceRouter
import com.bashkevich.tennisscorekeeperbackend.feature.match.doubles.DoublesMatchRepository
import com.bashkevich.tennisscorekeeperbackend.feature.match.doubles.DoublesMatchService
import com.bashkevich.tennisscorekeeperbackend.feature.match_log.DoublesMatchLogRepository
import com.bashkevich.tennisscorekeeperbackend.feature.match_log.SinglesMatchLogRepository
import com.bashkevich.tennisscorekeeperbackend.feature.match.singles.SinglesMatchRepository
import com.bashkevich.tennisscorekeeperbackend.feature.match.singles.SinglesMatchService
import com.bashkevich.tennisscorekeeperbackend.feature.set_template.SetTemplateRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val matchModule = module {
    singleOf(::MatchServiceRouter)
    singleOf(::SetTemplateRepository)

    singleOf(::SinglesMatchRepository)
    singleOf(::SinglesMatchLogRepository)
    singleOf(::SinglesMatchService)

    singleOf(::DoublesMatchRepository)
    singleOf(::DoublesMatchLogRepository)
    singleOf(::DoublesMatchService)
}