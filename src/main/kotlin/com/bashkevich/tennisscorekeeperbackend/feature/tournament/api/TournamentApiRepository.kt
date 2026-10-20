package com.bashkevich.tennisscorekeeperbackend.feature.tournament.api

import com.bashkevich.tennisscorekeeperbackend.model.tournament.api.TournamentApiRelationTable
import com.bashkevich.tennisscorekeeperbackend.model.tournament.api.TournamentApiTable
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentStatus
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentType
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

/** Строка кэш-таблицы турниров внешнего API (Лига тенниса). */
data class TournamentApiRecord(
    val apiId: String,
    val name: String,
    val status: TournamentStatus,
    val type: TournamentType,
    val dateStart: LocalDate,
)

class TournamentApiRepository {

    /**
     * Полная замена кэша: старый список удаляется, новый вставляется. Вызывается
     * внутри одной транзакции (dbQuery) — параллельные читатели видят только
     * закоммиченное состояние, без промежуточного пустого кэша.
     */
    suspend fun replaceAll(tournaments: List<TournamentApiRecord>) {
        TournamentApiTable.deleteAll()

        tournaments.forEach { record ->
            TournamentApiTable.insert {
                it[apiId] = record.apiId
                it[name] = record.name
                it[status] = record.status
                it[type] = record.type
                it[dateStart] = record.dateStart
            }
        }
    }

    suspend fun getByApiId(apiId: String): TournamentApiRecord? =
        TournamentApiTable.selectAll()
            .where { TournamentApiTable.apiId eq apiId }
            .map { it.toRecord() }
            .singleOrNull()

    /**
     * Предстоящие (NOT_STARTED) турниры кэша, ещё не добавленные в систему, — одним
     * запросом: LEFT JOIN с tournament_api_relation по api_id, неразвёрнутая связь
     * (id IS NULL) означает, что турнир ещё не добавлен.
     */
    suspend fun getUpcomingTournaments(): List<TournamentApiRecord> =
        TournamentApiTable
            .join(
                TournamentApiRelationTable,
                JoinType.LEFT,
                onColumn = TournamentApiTable.apiId,
                otherColumn = TournamentApiRelationTable.apiId,
            )
            .select(TournamentApiTable.columns)
            .where {
                (TournamentApiTable.status eq TournamentStatus.NOT_STARTED) and
                    TournamentApiRelationTable.tournamentId.isNull()
            }
            .orderBy(TournamentApiTable.dateStart, SortOrder.ASC)
            .map { it.toRecord() }

    private fun ResultRow.toRecord() = TournamentApiRecord(
        apiId = this[TournamentApiTable.apiId],
        name = this[TournamentApiTable.name],
        status = this[TournamentApiTable.status],
        type = this[TournamentApiTable.type],
        dateStart = this[TournamentApiTable.dateStart],
    )
}
