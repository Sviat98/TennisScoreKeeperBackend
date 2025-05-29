package com.bashkevich.tennisscorekeeperbackend.di

import com.bashkevich.tennisscorekeeperbackend.feature.participant.ParticipantServiceRouter
import com.bashkevich.tennisscorekeeperbackend.feature.participant.doubles.DoublesParticipantRepository
import com.bashkevich.tennisscorekeeperbackend.feature.participant.doubles.DoublesParticipantService
import com.bashkevich.tennisscorekeeperbackend.feature.participant.singles.SinglesParticipantRepository
import com.bashkevich.tennisscorekeeperbackend.feature.participant.singles.SinglesParticipantService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val participantModule = module {
    singleOf(::ParticipantServiceRouter)
    singleOf(::SinglesParticipantService)
    singleOf(::DoublesParticipantService)
    singleOf(::DoublesParticipantRepository)
    singleOf(::SinglesParticipantRepository)
    singleOf(::DoublesParticipantRepository)
}