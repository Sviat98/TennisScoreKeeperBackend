package com.bashkevich.tennisscorekeeperbackend.feature.tournament.api

import com.bashkevich.tennisscorekeeperbackend.model.tournament.api.TournamentApiRelationTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select

class TournamentApiRelationRepository {

    /** Все api_id, уже связанные с турнирами нашей системы (для отсечения добавленных турниров в GET /tournaments/active). */
    suspend fun getApiIds(): Set<String> =
        TournamentApiRelationTable
            .select(TournamentApiRelationTable.apiId)
            .map { it[TournamentApiRelationTable.apiId] }
            .toSet()

    /** Id турнира нашей системы, связанного с данным api_id, или null, если связи ещё нет. */
    suspend fun getTournamentIdByApiId(apiId: String): Int? =
        TournamentApiRelationTable
            .select(TournamentApiRelationTable.tournamentId)
            .where { TournamentApiRelationTable.apiId eq apiId }
            .map { it[TournamentApiRelationTable.tournamentId] }
            .singleOrNull()

    suspend fun addRelation(tournamentId: Int, apiId: String) {
        TournamentApiRelationTable.insert {
            it[TournamentApiRelationTable.tournamentId] = tournamentId
            it[TournamentApiRelationTable.apiId] = apiId
        }
    }
}
