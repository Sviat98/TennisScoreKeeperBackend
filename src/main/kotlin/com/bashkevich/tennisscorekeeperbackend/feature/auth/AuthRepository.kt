package com.bashkevich.tennisscorekeeperbackend.feature.auth

import com.bashkevich.tennisscorekeeperbackend.model.auth.PlayerAuthEntity
import com.bashkevich.tennisscorekeeperbackend.model.auth.PlayerAuthTable
import com.bashkevich.tennisscorekeeperbackend.model.auth.RefreshTokenTable
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

class AuthRepository {

    suspend fun getAuthInfoByLogin(login: String): PlayerAuthEntity? =
        PlayerAuthTable.selectAll().where { PlayerAuthTable.login eq login }
            .map { row ->
                PlayerAuthEntity(
                    id = row[PlayerAuthTable.playerId].value,
                    login = row[PlayerAuthTable.login],
                    hashedPassword = row[PlayerAuthTable.password],
                    isAdmin = row[PlayerAuthTable.isAdmin]
                )
            }.firstOrNull()

    suspend fun insertRefreshToken(playerId: Int, deviceId: UUID, token: String, expDateProjected: LocalDateTime) {
        RefreshTokenTable.insert {
            it[player] = playerId
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

    suspend fun makeRefreshTokenExpired(token: String, expDateReal: LocalDateTime) {
        RefreshTokenTable.update({ RefreshTokenTable.refreshToken eq token}) {
            it[expirationDateReal] = expDateReal
        }
    }
}