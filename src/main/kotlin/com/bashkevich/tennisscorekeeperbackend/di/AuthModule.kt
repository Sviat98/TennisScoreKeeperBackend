package com.bashkevich.tennisscorekeeperbackend.di


import com.bashkevich.tennisscorekeeperbackend.feature.auth.AuthRepository
import com.bashkevich.tennisscorekeeperbackend.feature.auth.AuthService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val authModule = module{
    singleOf(::AuthRepository)
    singleOf(::AuthService)
}