package com.bashkevich.tennisscorekeeperbackend.feature.participant.singles

import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.notInList
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

    fun getParticipantsByTournament(tournamentId: Int) : List<SinglesParticipantEntity> = SinglesParticipantEntity.find { SinglesParticipantTable.tournament eq tournamentId }.orderBy(
        SinglesParticipantTable.seed to SortOrder.ASC_NULLS_LAST,
        SinglesParticipantTable.id to SortOrder.ASC,
    ).toList()

    fun getParticipantById(participantId: Int) = SinglesParticipantEntity.findById(participantId)

    fun deleteUnnecessaryParticipants(tournamentId: Int, registeredParticipantIds: List<Int>) {
        SinglesParticipantTable.deleteWhere { (SinglesParticipantTable.tournament eq tournamentId) and (SinglesParticipantTable.id notInList registeredParticipantIds)}
    }
}