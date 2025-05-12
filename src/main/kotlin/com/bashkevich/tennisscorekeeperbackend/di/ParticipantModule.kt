package com.bashkevich.tennisscorekeeperbackend.di

import com.bashkevich.tennisscorekeeperbackend.feature.participant.doubles.DoublesParticipantRepository
import com.bashkevich.tennisscorekeeperbackend.feature.participant.singles.SinglesParticipantRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val participantModule = module {
    singleOf(::SinglesParticipantRepository)
    singleOf(::DoublesParticipantRepository)
}