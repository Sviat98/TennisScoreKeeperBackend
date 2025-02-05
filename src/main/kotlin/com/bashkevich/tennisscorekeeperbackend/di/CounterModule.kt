package com.bashkevich.tennisscorekeeperbackend.di

import com.bashkevich.tennisscorekeeperbackend.feature.counter.CounterRepository
import com.bashkevich.tennisscorekeeperbackend.feature.counter.CounterService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val counterModule = module{
    singleOf(::CounterRepository)
    singleOf(::CounterService)
}