package com.bashkevich.tennisscorekeeperbackend.plugins

import com.bashkevich.tennisscorekeeperbackend.model.message.ResponseMessageDto
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.MultiPartData
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException
import org.jetbrains.exposed.v1.dao.exceptions.EntityNotFoundException


fun Application.configureStatusPages(){
    install(StatusPages){
        exception<InvalidBodyException> { call, cause ->
            call.respondWithMessageBody(statusCode = HttpStatusCode.BadRequest, message = cause.message ?: "")
        }
        exception<RequestValidationException> { call, cause ->
            call.respondWithMessageBody(statusCode = HttpStatusCode.BadRequest, message = cause.message ?: "")
        }
        exception<BadRequestException> { call, cause ->
            call.respondWithMessageBody(statusCode = HttpStatusCode.BadRequest, message = cause.message ?: "")
        }
        exception<NotFoundException> { call, cause ->
            call.respondWithMessageBody(statusCode = HttpStatusCode.NotFound, message = cause.message ?: "")
        }
        exception<EntityNotFoundException>{ call, cause ->
            val entityId = cause.id
            val entityClass = cause.entity.javaClass

            call.respondWithMessageBody(statusCode = HttpStatusCode.NotFound, "Entity $entityClass with id = $entityId not found")
        }
        exception<Throwable> { call, cause ->
            call.respondWithMessageBody(statusCode = HttpStatusCode.InternalServerError, message = cause.message ?: "")
        }
    }
}
class InvalidBodyException(message: String = "Invalid body format in request!") : Exception(message)

suspend inline fun <reified T : Any> ApplicationCall.receiveBodyCatching(): T {
    return try {
        receive<T>()
    } catch (e: SerializationException) {
        throw InvalidBodyException("Invalid JSON format: ${e.message}")
    } catch (e: ContentTransformationException) {
        throw InvalidBodyException("Failed to parse body: ${e.message}")
    } catch (e: Exception) {
        throw InvalidBodyException("Unexpected error: ${e.message}")
    }
}

suspend inline fun ApplicationCall.receiveMultipartCatching(): MultiPartData {
    return try {
        receiveMultipart()
    } catch (e: Exception) {
        throw BadRequestException("Error receiving multipart data: ${e.message}")
    }
}

suspend inline fun ApplicationCall.respondWithMessageBody(
    statusCode: HttpStatusCode = HttpStatusCode.OK,
    message: String,
) {
    respond(statusCode, ResponseMessageDto(message))
}