package com.bashkevich.tennisscorekeeperbackend.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCors(){
    install(CORS){
        allowHost("127.0.0.1:8080")
        allowHost("127.0.0.1:8081")
        // Остальные разрешённые хосты задаются переменной среды — список через запятую, например
        // CORS_EXTRA_HOSTS=tennisscorekeeper.onrender.com,tennisscorekeeper.tech,api.tennisscorekeeper.tech,tennisscorekeeperbackend.onrender.com.
        // Через неё же задаются собственные публичные адреса API: страницы Swagger на проде шлют
        // такой Origin, но same-origin за TLS-прокси не распознаётся (сервер видит схему http, а не https)
        System.getenv("CORS_EXTRA_HOSTS")
            ?.split(',')
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?.forEach { allowHost(it) }
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