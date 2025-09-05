package com.bashkevich.tennisscorekeeperbackend.model.auth

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.datetime.datetime

object RefreshTokenTable : CompositeIdTable("refresh_token_info") {
    val player = reference("player_id", PlayerTable)
    val device = uuid("device_id").entityId()
    val refreshToken = varchar("refresh_token",500)
    val expirationDateProjected = datetime("expiration_date_projected")
    val expirationDateReal = datetime("expiration_date_real").nullable()

    override val primaryKey = PrimaryKey(player, device)
}