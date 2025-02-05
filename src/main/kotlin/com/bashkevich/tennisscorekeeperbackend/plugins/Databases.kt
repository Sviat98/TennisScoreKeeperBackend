package com.bashkevich.tennisscorekeeperbackend.plugins

import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterTable
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    val cloudUsername = System.getenv("CLOUD_USERNAME")

    val cloudPassword = System.getenv("CLOUD_PASSWORD")

    val cloudDB = System.getenv("CLOUD_DB")

    val schemaName = "tennis_score_keeper"

    val remotejdbcURL = "jdbc:postgresql://ep-blue-fog-a2izbdkl-pooler.eu-central-1.aws.neon.tech/$cloudDB?sslmode=require"

    val localUsername = System.getenv("LOCAL_USERNAME")

    val localPassword = System.getenv("LOCAL_PASSWORD")

    val localDB = System.getenv("LOCAL_DB")

    val localjdbcURL = "jdbc:postgresql://localhost:5432/$localDB"


    val driverClassName = "org.postgresql.Driver"

    val databaseConfig = DatabaseConfig {
        //keepLoadedReferencesOutOfTransaction = true
    }

    Database.connect(
        url = remotejdbcURL,
        driver = driverClassName,
        user = cloudUsername,
        password = cloudPassword,
        databaseConfig = databaseConfig
    )
    //Database.connect(url = localjdbcURL, driver = driverClassName, user = localUsername, password = localPassword, databaseConfig = databaseConfig)


    transaction {
        exec("SET search_path TO $schemaName;")
        SchemaUtils.create(
            CounterTable
        )
    }
}

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) {
        addLogger(StdOutSqlLogger)
        block()
    }
