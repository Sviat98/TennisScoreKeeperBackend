package com.bashkevich.tennisscorekeeperbackend.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.bashkevich.tennisscorekeeperbackend.model.auth.JWT_AUTH
import com.bashkevich.tennisscorekeeperbackend.model.auth.JwtConfig
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

fun Application.configureAuthentication(){
    JwtConfig.initFromConfig(environment.config)

    val jwtConfig = JwtConfig.instance
    val myRealm = environment.config.property("jwt.realm").getString()

    install(Authentication){
        jwt(JWT_AUTH) {
            realm = myRealm
            verifier(
                JWT
                .require(Algorithm.HMAC256(jwtConfig.secret))
                .withAudience(jwtConfig.audience)
                .withIssuer(jwtConfig.issuer)
                .build())
            validate { credential->
                JWTPrincipal(credential.payload)
            }
            challenge {_,_->
                throw UnauthorizedException("Token is not valid or has expired")
            }
        }
    }
}