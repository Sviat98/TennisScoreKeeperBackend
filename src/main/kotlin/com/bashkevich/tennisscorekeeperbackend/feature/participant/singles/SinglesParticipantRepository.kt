package com.bashkevich.tennisscorekeeperbackend.feature.participant.singles

import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantTable
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.upsert

class SinglesParticipantRepository {

    fun upsertParticipant(tournamentId: Int, participantSeed: Int?, playerId: Int): Int {
        return SinglesParticipantTable.upsert(
            SinglesParticipantTable.tournament, SinglesParticipantTable.player,
            onUpdate = {listOf(SinglesParticipantTable.seed to participantSeed)}
        ){
            it[tournament] = tournamentId
            it[seed] = participantSeed
            it[player] = playerId
        }[SinglesParticipantTable.id].value
    }

    fun getParticipantById(participantId: Int) = SinglesParticipantEntity.findById(participantId)

    fun deleteUnnecessaryParticipants(tournamentId: Int, registeredParticipantIds: List<Int>) {
        SinglesParticipantTable.deleteWhere { (SinglesParticipantTable.tournament eq tournamentId) and (SinglesParticipantTable.id notInList registeredParticipantIds)}
    }
}