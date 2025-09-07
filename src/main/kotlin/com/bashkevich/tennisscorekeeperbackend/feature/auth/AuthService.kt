package com.bashkevich.tennisscorekeeperbackend.feature.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.TokenExpiredException
import com.bashkevich.tennisscorekeeperbackend.model.auth.JwtConfig
import com.bashkevich.tennisscorekeeperbackend.model.auth.LoginResponse
import com.bashkevich.tennisscorekeeperbackend.model.auth.TokenType
import com.bashkevich.tennisscorekeeperbackend.plugins.UnauthorizedException
import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

class AuthService(
    private val authRepository: AuthRepository,
) {
    suspend fun login(login: String, password: String): LoginResponse {
        return dbQuery {
            val authInfo = authRepository.getAuthInfoByLogin(login) ?: throw NotFoundException("No user found")

            if (isUserVerified(authInfo.hashedPassword, password)) {
                val playerId = authInfo.id
                val deviceId = UUID.randomUUID()
                val accessTokenExpiresAt = calculateExpirationDate(TokenType.ACCESS)
                val refreshTokenExpiresAt = calculateExpirationDate(TokenType.REFRESH)

                println("accessTokenExpiresAt = $accessTokenExpiresAt")
                println("refreshTokenExpiresAt = $refreshTokenExpiresAt")


                val accessToken = signToken(playerId, deviceId, accessTokenExpiresAt)
                val refreshToken = signToken(playerId, deviceId, refreshTokenExpiresAt)

                authRepository.insertRefreshToken(
                    playerId = playerId,
                    deviceId = deviceId,
                    token = refreshToken,
                    expDateProjected = refreshTokenExpiresAt
                )

                LoginResponse(playerId = playerId.toString(), accessToken = accessToken, refreshToken = refreshToken)
            } else
                throw NotFoundException("Wrong login or password!")
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun logout(refreshToken: String?) {
        dbQuery {
            if (refreshToken == null) throw BadRequestException("Refresh token is not provided!")

            val expDateReal = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            authRepository.makeRefreshTokenExpired(refreshToken, expDateReal)
        }
    }

    suspend fun checkRefreshTokenIsValid(refreshToken: String?) {
        dbQuery {
            if (refreshToken == null) throw BadRequestException("Refresh token is not provided")

            if (!verifyRefreshToken(refreshToken)) throw UnauthorizedException("Refresh token expired")
        }
    }

    suspend fun refreshAccessToken(refreshToken: String?): LoginResponse {
        return dbQuery {
            if (refreshToken == null) throw BadRequestException("Refresh token is not provided")

            if (!verifyRefreshToken(refreshToken)) throw UnauthorizedException("Refresh token expired")

            val accessTokenExpDate = calculateExpirationDate(TokenType.ACCESS)

            val decodedToken = JWT.decode(refreshToken)
            val playerId = decodedToken.getClaim("playerId").toString().toInt()
            val deviceId = UUID.fromString(decodedToken.getClaim("deviceId").asString())

            val accessToken = signToken(playerId = playerId, deviceId = deviceId, expiresAt = accessTokenExpDate)

            LoginResponse(playerId = playerId.toString(), accessToken = accessToken, refreshToken = refreshToken)
        }
    }

    private fun isUserVerified(passwordFromDatabase: ByteArray, passwordFromRequest: String): Boolean {
        return BCrypt.verifyer().verify(
            passwordFromRequest.toByteArray(StandardCharsets.UTF_8),
            passwordFromDatabase
        ).verified
    }

    private fun signToken(playerId: Int, deviceId: UUID, expiresAt: LocalDateTime): String {
        val jwtConfig = JwtConfig.instance

        val timeZone = ZoneId.of("Europe/Minsk")

        return JWT.create()
            .withIssuer(jwtConfig.issuer)
            .withExpiresAt(expiresAt.toJavaLocalDateTime().atZone(timeZone).toInstant())
            .withClaim("playerId", playerId)
            .withClaim("deviceId", deviceId.toString())
            .withAudience(jwtConfig.audience)
            .sign(Algorithm.HMAC256(jwtConfig.secret))
    }

    @OptIn(ExperimentalTime::class)
    private fun calculateExpirationDate(tokenType: TokenType): LocalDateTime {
        val timeZone = TimeZone.of("Europe/Minsk")
        return when (tokenType) {
            TokenType.ACCESS -> Clock.System.now().plus(5.minutes).toLocalDateTime(timeZone)
            TokenType.REFRESH -> {
                val currentDateTime = Clock.System.now().toLocalDateTime(timeZone)

                val threeAM = LocalTime(3, 0)

                if (currentDateTime.time < threeAM) {
                    // Если сейчас до 3:00 - сегодня в 3:00
                    LocalDateTime(currentDateTime.date, threeAM)
                } else {
                    // Если сейчас после 3:00 - завтра в 3:00
                    LocalDateTime(currentDateTime.date.plus(1, DateTimeUnit.DAY), threeAM)
                }
            }
        }
    }

    private suspend fun verifyRefreshToken(refreshToken: String): Boolean {
        val jwtConfig = JwtConfig.instance

        val refreshTokenVerifier = JWT
            .require(Algorithm.HMAC256(jwtConfig.secret))
            .withAudience(jwtConfig.audience)
            .withIssuer(jwtConfig.issuer)
            .build()

        return try {
            refreshTokenVerifier.verify(refreshToken)
            // если возвращается true, значит токен был отозван ДО момента истечения, и считается невалидным
            !authRepository.checkRefreshTokenIsRevoked(refreshToken)
        } catch (e: TokenExpiredException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}