package com.bashkevich.tennisscorekeeperbackend.feature.player

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerEntity
import kotlinx.datetime.LocalDate


class PlayerRepository {

    fun addPlayer(playerSurname: String, playerName: String, playerDateBirth: LocalDate): PlayerEntity =
        PlayerEntity.new {
            surname = playerSurname
            name = playerName
            dateBirth = playerDateBirth
        }

    /**
     * Обновляет данные игрока (например, по свежему ответу API Лиги тенниса).
     * null — игрока с таким id нет.
     */
    fun updatePlayer(playerId: Int, playerSurname: String, playerName: String, playerDateBirth: LocalDate): PlayerEntity? =
        PlayerEntity.findById(playerId)?.apply {
            surname = playerSurname
            name = playerName
            dateBirth = playerDateBirth
        }

    fun getPlayerById(id: Int) = PlayerEntity.findById(id)

    fun getPlayers(): List<PlayerEntity> = PlayerEntity.all().toList()
}