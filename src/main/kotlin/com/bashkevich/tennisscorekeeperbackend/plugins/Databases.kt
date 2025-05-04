package com.bashkevich.tennisscorekeeperbackend.plugins

import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterTable
import com.bashkevich.tennisscorekeeperbackend.model.match_log.MatchLogTable
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchTable
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateTable
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    val cloudUsername = System.getenv("CLOUD_USERNAME")

    val cloudPassword = System.getenv("CLOUD_PASSWORD")

    val cloudDB = System.getenv("CLOUD_DB")

    val defaultSchemaName = "public"

    val schemaName = "tennis_score_keeper"

    val remotejdbcURL =
        "jdbc:postgresql://ep-blue-fog-a2izbdkl-pooler.eu-central-1.aws.neon.tech/$cloudDB?sslmode=require"

    val localUsername = System.getenv("LOCAL_USERNAME")

    val localPassword = System.getenv("LOCAL_PASSWORD")

    val localDB = System.getenv("LOCAL_DB")

    val localjdbcURL = "jdbc:postgresql://localhost:5432/$localDB"


    val driverClassName = "org.postgresql.Driver"

    //val schema = Schema(schemaName) // напоянтная ошибка с переключением схем БД, пока убрал это

    val schema = Schema(defaultSchemaName)
    val databaseConfig = DatabaseConfig {
        defaultSchema = schema
    }

    Database.connect(
        url = remotejdbcURL,
        driver = driverClassName,
        user = cloudUsername,
        password = cloudPassword,
        databaseConfig = databaseConfig,
    )
    //Database.connect(url = localjdbcURL, driver = driverClassName, user = localUsername, password = localPassword, databaseConfig = databaseConfig)


    transaction {
        SchemaUtils.create(
            CounterTable, PlayerTable, MatchTable, MatchLogTable, SetTemplateTable
        )

//        SetTemplateTable.insert {
//            it[gamesToWin] = 6
//            it[decidingPoint] = false
//            it[tiebreakMode] = TiebreakMode.LATE
//            it[tiebreakPointsToWin] = 7
//        }
//        SetTemplateTable.insert {
//            it[gamesToWin] = 6
//            it[decidingPoint] = true
//            it[tiebreakMode] = TiebreakMode.LATE
//            it[tiebreakPointsToWin] = 7
//        }
//        // Супер-тай-брейк до 10
//        SetTemplateTable.insert {
//            it[gamesToWin] = 1
//            it[decidingPoint] = false
//            it[tiebreakMode] = TiebreakMode.EARLY
//            it[tiebreakPointsToWin] = 10
//        }
    }
}

suspend fun isDbConnected(): Boolean {
    return try {
        newSuspendedTransaction(Dispatchers.IO) {
            exec("SELECT 1")
            true
        }
    } catch (e: Exception) {
        false
    }
}

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) {
        addLogger(StdOutSqlLogger)
        block()
    }
