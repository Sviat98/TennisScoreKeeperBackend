package com.bashkevich.tennisscorekeeperbackend.feature.player

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerBodyDto
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerDto
import com.bashkevich.tennisscorekeeperbackend.model.player.toDto
import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery
import io.ktor.server.plugins.NotFoundException

class PlayerService(
    private val playerRepository: PlayerRepository,
) {

    suspend fun addPlayer(playerBodyDto: PlayerBodyDto): PlayerDto {
        return dbQuery {
            playerRepository.addPlayer(
                playerSurname = playerBodyDto.surname,
                playerName = playerBodyDto.name,
                playerDateBirth = playerBodyDto.dateBirth
            ).toDto()
        }
    }

    suspend fun getPlayerById(id: Int): PlayerDto {
        return dbQuery {
            playerRepository.getPlayerById(id)?.toDto() ?: throw NotFoundException("Player not found")
        }
    }
}