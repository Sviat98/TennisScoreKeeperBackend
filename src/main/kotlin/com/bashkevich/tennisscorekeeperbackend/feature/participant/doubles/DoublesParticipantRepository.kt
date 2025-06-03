package com.bashkevich.tennisscorekeeperbackend.feature.participant.doubles

import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantTable
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.upsert

class DoublesParticipantRepository {
        fun upsertParticipant(tournamentId: Int, participantSeed: Int?, firstPlayerId: Int, secondPlayerId: Int, displayOrderFlag: Boolean): Int {
                return DoublesParticipantTable.upsert(
                        DoublesParticipantTable.tournament, DoublesParticipantTable.firstPlayer, DoublesParticipantTable.secondPlayer,
                        onUpdate = {listOf(DoublesParticipantTable.seed to participantSeed, DoublesParticipantTable.saveOrderAtDisplay to displayOrderFlag)}
                ){
                        it[tournament] = tournamentId
                        it[seed] = participantSeed
                        it[firstPlayer] = firstPlayerId
                        it[secondPlayer] = secondPlayerId
                        it[saveOrderAtDisplay] = displayOrderFlag
                }[DoublesParticipantTable.id].value
        }

        fun getParticipantById(participantId: Int) = DoublesParticipantEntity.findById(participantId)

        fun deleteUnnecessaryParticipants(tournamentId: Int, registeredParticipantIds: List<Int>) {
                DoublesParticipantTable.deleteWhere { (DoublesParticipantTable.tournament eq tournamentId) and (DoublesParticipantTable.id notInList registeredParticipantIds)}
        }
}