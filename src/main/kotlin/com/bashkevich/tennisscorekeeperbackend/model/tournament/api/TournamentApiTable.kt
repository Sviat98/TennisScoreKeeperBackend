package com.bashkevich.tennisscorekeeperbackend.model.tournament.api

import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentStatus
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.date

/**
 * Кэш-таблица турниров из внешнего API Лиги тенниса. Полностью перечитывается
 * планировщиком раз в час (старый список удаляется, новый вставляется — в одной
 * транзакции). Из кэша работают GET /tournaments/active и валидация api_id при
 * создании турнира — без похода в сеть в момент запроса.
 * status — код внешнего API, смаппленный при вставке: 1=NOT_STARTED, 2=IN_PROGRESS.
 */
object TournamentApiTable : Table("tournament_api") {
    val apiId = varchar("api_id", 50)
    val name = varchar("name", 255)
    val status = enumerationByName<TournamentStatus>("status", 50)
    val type = enumerationByName<TournamentType>("type", 50)
    val dateStart = date("date_start")

    override val primaryKey = PrimaryKey(apiId)
}
