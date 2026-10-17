package com.bashkevich.tennisscorekeeperbackend.model.tournament

import org.jetbrains.exposed.v1.core.Table

/**
 * Связь турнира нашей системы с турниром внешнего API (Лига тенниса).
 * id — идентификатор турнира из таблицы tournament (первичный ключ, без автоинкремента),
 * api_id — идентификатор турнира во внешней системе (уникальный индекс).
 */
object TournamentApiRelationTable : Table("tournament_api_relation") {
    val tournamentId = integer("id")
    val apiId = varchar("api_id", 50).uniqueIndex()

    override val primaryKey = PrimaryKey(tournamentId)
}
