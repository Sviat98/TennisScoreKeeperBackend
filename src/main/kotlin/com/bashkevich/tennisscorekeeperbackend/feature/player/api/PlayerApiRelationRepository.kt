package com.bashkevich.tennisscorekeeperbackend.feature.player.api

import com.bashkevich.tennisscorekeeperbackend.model.player.api.PlayerApiRelationTable
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select

/**
 * Связи игроков нашей системы с игроками внешнего API Лиги тенниса.
 * Связи везде передаются парами (apiId, playerId).
 */
class PlayerApiRelationRepository {

    /** Связи для перечисленных api_id (например, всех игроков загружаемого турнира). */
    suspend fun getPlayerIdsByApiIds(apiIds: Collection<Int>): List<Pair<Int, Int>> =
        PlayerApiRelationTable
            .select(PlayerApiRelationTable.apiId, PlayerApiRelationTable.playerId)
            .where { PlayerApiRelationTable.apiId inList apiIds.toList() }
            .map { it[PlayerApiRelationTable.apiId] to it[PlayerApiRelationTable.playerId] }

    /** Все связи (для предзаполнения: отсечь уже связанных игроков и занятые api_id). */
    suspend fun getRelations(): List<Pair<Int, Int>> =
        PlayerApiRelationTable
            .select(PlayerApiRelationTable.apiId, PlayerApiRelationTable.playerId)
            .map { it[PlayerApiRelationTable.apiId] to it[PlayerApiRelationTable.playerId] }

    suspend fun addRelation(playerId: Int, apiId: Int) {
        PlayerApiRelationTable.insert {
            it[PlayerApiRelationTable.playerId] = playerId
            it[PlayerApiRelationTable.apiId] = apiId
        }
    }
}
