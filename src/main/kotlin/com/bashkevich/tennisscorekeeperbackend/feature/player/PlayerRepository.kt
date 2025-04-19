package com.bashkevich.tennisscorekeeperbackend.feature.player

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerEntity


class PlayerRepository {

    fun addPlayer(playerSurname: String, playerName: String): PlayerEntity =
        PlayerEntity.new {
            surname = playerSurname
            name = playerName
        }

    fun getPlayerById(id: Int) = PlayerEntity.findById(id)
}