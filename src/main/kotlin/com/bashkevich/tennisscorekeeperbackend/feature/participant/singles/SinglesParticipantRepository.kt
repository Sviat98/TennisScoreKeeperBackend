package com.bashkevich.tennisscorekeeperbackend.feature.participant.singles

import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantEntity

class SinglesParticipantRepository {
    fun getParticipantById(participantId: Int) = SinglesParticipantEntity.Companion.findById(participantId)
}