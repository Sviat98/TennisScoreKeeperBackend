package com.bashkevich.tennisscorekeeperbackend.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCors(){
    install(CORS){
        allowHost("tennisscorekeeper.onrender.com")
        allowHost("tennisscorekeeper.tech")
        allowHost("localhost:8080")
        allowHost("localhost:8081")
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        //By default, the CORS plugin allows the GET, POST and HEAD HTTP methods
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowSameOrigin
    }
}