package com.bashkevich.tennisscorekeeperbackend.feature.participant.singles

import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantTable
import org.jetbrains.exposed.v1.jdbc.upsert

class SinglesParticipantRepository {
//    fun upsertParticipant(tournamentId: Int, playerSeed: Int?, playerId: Int){
//        SinglesParticipantTable.upsert(
//            SinglesParticipantTable.tournament,
//            SinglesParticipantTable.player,
//            onUpdate = {updateStatement ->
//                        updateStatement[seed] = playerSeed
//            }
//        ){
//            it[tournament] = tournamentId
//            it[seed] = playerSeed
//            it[player] = playerId
//        }
//
//    }

    fun upsertParticipant(tournamentId: Int, playerSeed: Int?, playerId: Int): Int {
        return SinglesParticipantTable.upsert(
            SinglesParticipantTable.tournament, SinglesParticipantTable.player,
            onUpdate = {listOf(SinglesParticipantTable.seed to playerSeed)}
        ){
            it[tournament] = tournamentId
            it[seed] = playerSeed
            it[player] = playerId
        }[SinglesParticipantTable.id].value
    }

    fun getParticipantById(participantId: Int) = SinglesParticipantEntity.findById(participantId)
}