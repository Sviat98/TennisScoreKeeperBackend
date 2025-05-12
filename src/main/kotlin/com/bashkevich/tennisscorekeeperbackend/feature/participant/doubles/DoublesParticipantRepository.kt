package com.bashkevich.tennisscorekeeperbackend.feature.participant.doubles

import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantEntity

class DoublesParticipantRepository {
        fun getParticipantById(participantId: Int) = DoublesParticipantEntity.findById(participantId)
}