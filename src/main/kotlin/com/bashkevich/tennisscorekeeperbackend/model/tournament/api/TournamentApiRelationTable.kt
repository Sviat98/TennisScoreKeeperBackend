package com.bashkevich.tennisscorekeeperbackend.model.tournament.api

import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentTable
import org.jetbrains.exposed.v1.core.Table

/**
 * Связь турнира нашей системы с турниром внешнего API (Лига тенниса).
 * id — идентификатор турнира из таблицы tournament (первичный ключ, без автоинкремента,
 * с внешним ключом на tournament.id), api_id — идентификатор турнира во внешней системе
 * (уникальный индекс).
 */
object TournamentApiRelationTable : Table("tournament_api_relation") {
    val tournamentId = integer("id").references(TournamentTable.id)
    val apiId = varchar("api_id", 50).uniqueIndex()

    override val primaryKey = PrimaryKey(tournamentId)
}
