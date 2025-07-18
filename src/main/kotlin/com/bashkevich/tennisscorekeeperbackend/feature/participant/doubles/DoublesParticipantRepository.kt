package com.bashkevich.tennisscorekeeperbackend.feature.participant.doubles

import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.upsert

class DoublesParticipantRepository {
    suspend fun upsertParticipant(
        tournamentId: Int,
        participantSeed: Int?,
        firstPlayerId: Int,
        secondPlayerId: Int,
        displayOrderFlag: Boolean,
    ): Int {
        return DoublesParticipantTable.upsert(
            DoublesParticipantTable.tournament,
            DoublesParticipantTable.firstPlayer,
            DoublesParticipantTable.secondPlayer,
            onUpdate = {
                listOf(
                    DoublesParticipantTable.seed to participantSeed,
                    DoublesParticipantTable.saveOrderAtDisplay to displayOrderFlag
                )
            }
        ) {
            it[tournament] = tournamentId
            it[seed] = participantSeed
            it[firstPlayer] = firstPlayerId
            it[secondPlayer] = secondPlayerId
            it[saveOrderAtDisplay] = displayOrderFlag
        }[DoublesParticipantTable.id].value
    }

    fun getParticipantsByTournament(tournamentId: Int): List<DoublesParticipantEntity> =
        DoublesParticipantEntity.find { DoublesParticipantTable.tournament eq tournamentId }.orderBy(
            DoublesParticipantTable.seed to SortOrder.ASC_NULLS_LAST,
            DoublesParticipantTable.id to SortOrder.ASC,
            ).toList()

    fun getParticipantById(participantId: Int) = DoublesParticipantEntity.findById(participantId)

    suspend fun deleteUnnecessaryParticipants(tournamentId: Int, registeredParticipantIds: List<Int>) {
        DoublesParticipantTable.deleteWhere { (DoublesParticipantTable.tournament eq tournamentId) and (DoublesParticipantTable.id notInList registeredParticipantIds) }
    }
}