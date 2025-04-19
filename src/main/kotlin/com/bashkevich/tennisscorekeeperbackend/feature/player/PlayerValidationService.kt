package com.bashkevich.tennisscorekeeperbackend.feature.player

import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery

class PlayerValidationService(
    private val playerRepository: PlayerRepository
) {

    suspend fun validatePlayers(playerIds: List<Int>): Int?{
        return dbQuery {
            var invalidId: Int? = null

            playerIds.forEach { playerId->
                val playerEntity = playerRepository.getPlayerById(playerId)

                if (playerEntity==null){
                    invalidId = playerId
                    return@forEach
                }

            }
            invalidId
        }
    }
}