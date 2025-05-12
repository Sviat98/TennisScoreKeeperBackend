package com.bashkevich.tennisscorekeeperbackend.di

import com.bashkevich.tennisscorekeeperbackend.feature.tournament.TournamentRepository
import com.bashkevich.tennisscorekeeperbackend.feature.tournament.TournamentService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val tournamentModule = module {
    singleOf(::TournamentService)
    singleOf(::TournamentRepository)
}