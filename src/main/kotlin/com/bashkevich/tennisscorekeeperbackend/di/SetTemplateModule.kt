package com.bashkevich.tennisscorekeeperbackend.di

import com.bashkevich.tennisscorekeeperbackend.feature.set_template.SetTemplateRepository
import com.bashkevich.tennisscorekeeperbackend.feature.set_template.SetTemplateService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val setTemplateModule = module {
    singleOf(::SetTemplateService)
    singleOf(::SetTemplateRepository)
}