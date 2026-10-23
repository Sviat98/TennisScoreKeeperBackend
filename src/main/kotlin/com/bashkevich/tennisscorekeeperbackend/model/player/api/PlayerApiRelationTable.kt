package com.bashkevich.tennisscorekeeperbackend.model.player.api

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable
import org.jetbrains.exposed.v1.core.Table

/**
 * Связь игрока нашей системы с игроком внешнего API Лиги тенниса.
 * id — идентификатор игрока из таблицы player (первичный ключ, без автоинкремента,
 * с внешним ключом на player.id), api_id — идентификатор игрока во внешней системе
 * (уникальный индекс).
 */
object PlayerApiRelationTable : Table("player_api_relation") {
    val playerId = integer("id").references(PlayerTable.id)
    val apiId = integer("api_id").uniqueIndex()

    override val primaryKey = PrimaryKey(playerId)
}
