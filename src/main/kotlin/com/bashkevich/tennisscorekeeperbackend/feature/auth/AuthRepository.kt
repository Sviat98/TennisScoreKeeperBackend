package com.bashkevich.tennisscorekeeperbackend.feature.auth

import com.bashkevich.tennisscorekeeperbackend.model.auth.RefreshTokenTable
import com.bashkevich.tennisscorekeeperbackend.model.auth.UserAuthEntity
import com.bashkevich.tennisscorekeeperbackend.model.auth.UserAuthTable
import com.bashkevich.tennisscorekeeperbackend.model.auth.UserEntity
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

class AuthRepository {

    suspend fun getAuthInfoByLogin(login: String): UserAuthEntity? =
        UserAuthTable.selectAll().where { UserAuthTable.login eq login }
            .map { row ->
                UserAuthEntity(
                    userId = row[UserAuthTable.userId].value,
                    login = row[UserAuthTable.login],
                    hashedPassword = row[UserAuthTable.password],
                    isAdmin = row[UserAuthTable.isAdmin]
                )
            }.firstOrNull()

    suspend fun getUserById(id: Int): UserEntity? = UserEntity.findById(id)

    @OptIn(ExperimentalUuidApi::class)
    suspend fun insertRefreshToken(userId: Int, deviceId: Uuid, token: String, expDateProjected: LocalDateTime) {
        RefreshTokenTable.insert {
            it[user] = userId
            it[device] = deviceId
            it[refreshToken] = token
            it[expirationDateProjected] = expDateProjected
            it[expirationDateReal] = null
        }
    }

    suspend fun checkRefreshTokenIsRevoked(token: String): Boolean {
        val expDateReal = RefreshTokenTable.select(RefreshTokenTable.expirationDateReal).where { RefreshTokenTable.refreshToken eq token }.map { it[RefreshTokenTable.expirationDateReal] }.firstOrNull()

        return expDateReal!=null
    }

    suspend fun removeAllExpiredRefreshTokens(dateTime: LocalDateTime) {
        RefreshTokenTable.deleteWhere { RefreshTokenTable.expirationDateProjected less dateTime }
    }

    suspend fun makeRefreshTokenExpired(token: String, expDateReal: LocalDateTime) {
        RefreshTokenTable.update({ RefreshTokenTable.refreshToken eq token}) {
            it[expirationDateReal] = expDateReal
        }
    }
}