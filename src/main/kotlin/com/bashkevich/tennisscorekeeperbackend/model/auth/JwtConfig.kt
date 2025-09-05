package com.bashkevich.tennisscorekeeperbackend.model.auth

import io.ktor.server.config.ApplicationConfig

const val JWT_AUTH = "jwt-auth"

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
) {
    companion object {
        private var _instance: JwtConfig? = null
        val instance: JwtConfig get() = _instance ?: throw IllegalStateException("JwtConfig not initialized")

        fun initFromConfig(config: ApplicationConfig) {
            _instance = JwtConfig(
                secret = config.property("jwt.secret").getString(),
                issuer = config.property("jwt.issuer").getString(),
                audience = config.property("jwt.audience").getString(),
                realm = config.property("jwt.realm").getString(),
            )
        }
    }
}