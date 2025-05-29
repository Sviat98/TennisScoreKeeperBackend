package com.bashkevich.tennisscorekeeperbackend.feature.player

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerEntity
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.upperCase


class PlayerRepository {

    fun addPlayer(playerSurname: String, playerName: String, playerDateBirth: LocalDate): PlayerEntity =
        PlayerEntity.new {
            surname = playerSurname
            name = playerName
            dateBirth = playerDateBirth
        }

    fun getPlayerById(id: Int) = PlayerEntity.findById(id)

    fun searchPlayer(
        surname: String,
        name: String,
        dateBirth: LocalDate,
    ) =
        PlayerEntity.find { (PlayerTable.surname.upperCase() eq surname.uppercase()) and (PlayerTable.name.upperCase() eq name.uppercase()) and (PlayerTable.dateBirth eq dateBirth) }.firstOrNull()
}