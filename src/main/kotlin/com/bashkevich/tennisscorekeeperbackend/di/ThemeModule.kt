package com.bashkevich.tennisscorekeeperbackend.di

import com.bashkevich.tennisscorekeeperbackend.feature.theme.ThemeRepository
import com.bashkevich.tennisscorekeeperbackend.feature.theme.ThemeService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val themeModule = module {
    singleOf(::ThemeService)
    singleOf(::ThemeRepository)
}
