package com.bashkevich.tennisscorekeeperbackend.feature

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.utils.io.readRemaining
import kotlinx.datetime.LocalDateTime
import kotlinx.io.readByteArray
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.first
import org.jetbrains.kotlinx.dataframe.api.map
import org.jetbrains.kotlinx.dataframe.io.readExcel
import java.io.ByteArrayInputStream

fun Route.uploadRoutes(){
    post("/upload"){


        // Получаем MultipartData
            val multipart = call.receiveMultipart()
            var excelBytes: ByteArray? = null
        val log = call.application.environment.log

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        // Проверяем, что это файл Excel
                        if (part.name == "excelFile" && part.contentType == ContentType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {

                            excelBytes = part.provider().readRemaining().readByteArray() // Read all bytes into memory
                        } else {
                            // Опционально: обработать другие типы файлов или поля
                            log.warn("Received unexpected file part: ${part.name} with content type ${part.contentType}")
                        }
                    }
                    is PartData.FormItem -> {
                        // Опционально: обработать другие поля формы, если они есть
                        log.info("Received form item: ${part.name} = ${part.value}")
                    }
                    else -> {} // Игнорируем другие типы частей (например, BinaryItem)
                }
                part.dispose() // Важно освободить ресурсы части
            }

            if (excelBytes == null) {
                call.respond(HttpStatusCode.BadRequest,"No Excel file found in request or incorrect part name/content type. Please use 'excelFile' as field name and application/vnd.openxmlformats-officedocument.spreadsheetml.sheet as content type.")
                return@post
            }

            try {
                val inputStream = ByteArrayInputStream(excelBytes)

                // Используем Kotlin Dataframe для чтения Excel из InputStream
                val df = DataFrame.readExcel(ByteArrayInputStream(excelBytes))

                val seedDf = DataFrame.readExcel(ByteArrayInputStream(excelBytes,), firstRowIsHeader = false).first()

                val playersDf = DataFrame.readExcel(inputStream = ByteArrayInputStream(excelBytes), skipRows = 1, firstRowIsHeader = true)

                println(seedDf[0].toString())

                val participants = playersDf.map { row->
                    val name = row["Имя"]?.toString() ?: ""
                    val surname = row["Фамилия"]?.toString() ?: ""
                    val dateBirth = row["Дата рождения"]?.toString() ?: ""

                    "$name $surname ${LocalDateTime.parse(dateBirth).date}"
                }

                // Парсим данные и создаем список участников
//                val participants = mutableListOf<String>()
//                df.map { it }
//                df.forEach { row ->
//                    try {
//                        val name = row["Имя"]?.toString() ?: ""
//                        val surname = row["Фамилия"]?.toString() ?: ""
//                        val dateBirth = row["Дата рождения"]?.toString() ?: ""
//
//                        // Дополнительная валидация, если поля обязательны
//                        if (name.isNotBlank()) {
//                            participants.add("$name $surname $dateBirth")
//                        } else {
//                            //val seedAmount = row[0]?.toString()?.split(":")[1]
//
//                            //println("Seed amount: $seedAmount")
//                            log.warn("Skipping row due to missing 'Имя' or 'Email': $row")
//                        }
//                    } catch (e: Exception) {
//                        log.error("Error parsing row: $row", e)
//                        // Можно пропустить проблемную строку или добавить в список ошибок
//                    }
//                }

                if (participants.isEmpty()) {
                    call.respond(HttpStatusCode.NotFound,"No participants found or parsed from the Excel file. Please check column names 'Имя' and 'Email'.")
                } else {
                    participants.forEach {
                        println(it)
                    }
                    call.respondText("Parsing successfull") // Отправляем список участников обратно клиенту
                }

            } catch (e: Exception) {
                log.error("Error processing Excel file: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError,"Error processing Excel file: ${e.message}")
            }
        }
}