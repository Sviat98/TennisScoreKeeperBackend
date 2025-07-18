package com.bashkevich.tennisscorekeeperbackend.plugins

import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterTable
import com.bashkevich.tennisscorekeeperbackend.model.match.doubles.DoublesMatchTable
import com.bashkevich.tennisscorekeeperbackend.model.match_log.singles.SinglesMatchLogTable
import com.bashkevich.tennisscorekeeperbackend.model.match.singles.SinglesMatchTable
import com.bashkevich.tennisscorekeeperbackend.model.match_log.doubles.DoublesMatchLogTable
import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantTable
import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantTable
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateTable
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.core.Sequence
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

const val MATCH_SEQUENCE = "match_seq"
const val PARTICIPANT_SEQUENCE = "participant_seq"

suspend fun configureDatabase() {
    val cloudUsername = System.getenv("CLOUD_USERNAME")

    val cloudPassword = System.getenv("CLOUD_PASSWORD")

    val cloudDB = System.getenv("CLOUD_DB")

    val defaultSchemaName = "public"

    val schemaName = "tennis_score_keeper"

    val remotejdbcURL =
        "jdbc:postgresql://ep-blue-fog-a2izbdkl-pooler.eu-central-1.aws.neon.tech/$cloudDB?sslmode=require"

    val remoter2dbcURL =
        "r2dbc:postgresql://ep-blue-fog-a2izbdkl-pooler.eu-central-1.aws.neon.tech/$cloudDB?sslmode=require"

    val localUsername = System.getenv("LOCAL_USERNAME")

    val localPassword = System.getenv("LOCAL_PASSWORD")

    val localDB = System.getenv("LOCAL_DB")

    val localjdbcURL = "jdbc:postgresql://localhost:5432/$localDB"


    val driverClassName = "org.postgresql.Driver"
    val r2dbcDriverClassName = "io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider"


    //val schema = Schema(schemaName) // напоянтная ошибка с переключением схем БД, пока убрал это

    val schema = Schema(defaultSchemaName)

    R2dbcDatabase.connect(
        url = remoter2dbcURL,
        user = cloudUsername,
        password = cloudPassword,
        databaseConfig = {
            defaultSchema = schema
        }
    )
    //Database.connect(url = localjdbcURL, driver = driverClassName, user = localUsername, password = localPassword, databaseConfig = databaseConfig)


    val matchSequence = Sequence(MATCH_SEQUENCE)
    val participantSequence = Sequence(PARTICIPANT_SEQUENCE)

    suspendTransaction(Dispatchers.IO) {
        SchemaUtils.createSequence(matchSequence, participantSequence)

        SchemaUtils.create(
            CounterTable, PlayerTable, SetTemplateTable, TournamentTable,
            SinglesParticipantTable, SinglesMatchTable, SinglesMatchLogTable,
            DoublesParticipantTable, DoublesMatchTable, DoublesMatchLogTable
        )
    }

//        SetTemplateEntity.new {
//            name = "До 6 геймов;Тай-брейк до 7 при счете 6-6"
//            gamesToWin = 6
//            decidingPoint = false
//            tiebreakMode = TiebreakMode.LATE
//            tiebreakPointsToWin = 7
//            isRegularSet = true
//            isDecidingSet = true
//        }
//        SetTemplateEntity.new {
//            name = "До 6 геймов;Тай-брейк до 7 при счете 6-6;При счете ровно решающее очко"
//            gamesToWin = 6
//            decidingPoint = true
//            tiebreakMode = TiebreakMode.LATE
//            tiebreakPointsToWin = 7
//            isRegularSet = true
//            isDecidingSet = true
//        }
//        // Супер-тай-брейк до 10
//        SetTemplateEntity.new {
//            name = "Супер тай-брейк до 10"
//            gamesToWin = 1
//            decidingPoint = false
//            tiebreakMode = TiebreakMode.EARLY
//            tiebreakPointsToWin = 10
//            isRegularSet = true
//            isDecidingSet = true
//        }
}

suspend fun isDbConnected(): Boolean {
    return try {
        suspendTransaction(Dispatchers.IO) {
            exec("SELECT 1")
            true
        }
    } catch (e: Exception) {
        false
    }
}

suspend fun <T> dbQuery(block: suspend () -> T): T =
    suspendTransaction(Dispatchers.IO) {
        addLogger(StdOutSqlLogger)
        block()
    }
