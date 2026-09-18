package com.bashkevich.tennisscorekeeperbackend.model.auth

import kotlin.uuid.ExperimentalUuidApi
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.datetime.datetime

@OptIn(ExperimentalUuidApi::class)
object RefreshTokenTable : CompositeIdTable("refresh_token_info") {
    val user = reference("user_id", UserTable)
    val device = uuid("device_id").entityId()
    val refreshToken = varchar("refresh_token",500)
    val expirationDateProjected = datetime("expiration_date_projected")
    val expirationDateReal = datetime("expiration_date_real").nullable()
    override val primaryKey = PrimaryKey(user, device)
}