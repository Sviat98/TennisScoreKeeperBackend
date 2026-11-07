package com.bashkevich.tennisscorekeeperbackend.feature.tournament.api

import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentStatus
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentTable
import com.bashkevich.tennisscorekeeperbackend.model.tournament.api.TournamentApiRelationTable
import com.bashkevich.tennisscorekeeperbackend.model.tournament.api.TournamentApiTable
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.neq
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

    /** api_id турнира внешнего API, связанного с данным турниром нашей системы, или null, если связи нет. */
    suspend fun getApiIdByTournamentId(tournamentId: Int): String? =
        TournamentApiRelationTable
            .select(TournamentApiRelationTable.apiId)
            .where { TournamentApiRelationTable.tournamentId eq tournamentId }
            .map { it[TournamentApiRelationTable.apiId] }
            .singleOrNull()

    /**
     * Турниры не в статусе COMPLETED, связанные с внешним API, которых больше нет в
     * кэш-таблице tournament_api: inner join со связью отбирает только связанные с API
     * турниры, LEFT JOIN с кэшем по api_id (api_id IS NULL) — исчезнувшие из кэша.
     * Возвращает пары (id турнира, api_id).
     */
    suspend fun getNotCompletedTournamentsMissingInApiCache(): List<Pair<Int, String>> =
        (TournamentTable innerJoin TournamentApiRelationTable)
            .join(
                TournamentApiTable,
                JoinType.LEFT,
                onColumn = TournamentApiRelationTable.apiId,
                otherColumn = TournamentApiTable.apiId,
            )
            .select(TournamentApiRelationTable.tournamentId, TournamentApiRelationTable.apiId)
            .where {
                (TournamentTable.status neq TournamentStatus.COMPLETED) and
                    TournamentApiTable.apiId.isNull()
            }
            .map { it[TournamentApiRelationTable.tournamentId] to it[TournamentApiRelationTable.apiId] }

    suspend fun addRelation(tournamentId: Int, apiId: String) {
        TournamentApiRelationTable.insert {
            it[TournamentApiRelationTable.tournamentId] = tournamentId
            it[TournamentApiRelationTable.apiId] = apiId
        }
    }
}
