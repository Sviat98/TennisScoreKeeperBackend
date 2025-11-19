package com.bashkevich.tennisscorekeeperbackend.model.auth

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable
import org.jetbrains.exposed.v1.core.Table

object PlayerAuthTable: Table("player_auth"){
    val playerId = reference("player_id", PlayerTable)
    val login = varchar("login", 50)
    val password = binary("password")
    val isAdmin = bool("is_admin")

    override val primaryKey = PrimaryKey(playerId)
}