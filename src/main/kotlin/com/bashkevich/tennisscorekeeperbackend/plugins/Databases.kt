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
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateEntity
import com.bashkevich.tennisscorekeeperbackend.model.set_template.TiebreakMode
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentTable
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

const val MATCH_SEQUENCE = "match_seq"
const val PARTICIPANT_SEQUENCE = "participant_seq"

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



    val matchSequence = Sequence(MATCH_SEQUENCE)
    val participantSequence = Sequence(PARTICIPANT_SEQUENCE)

    transaction {
        SchemaUtils.createSequence(matchSequence,participantSequence)

        SchemaUtils.create(
            CounterTable, PlayerTable, SetTemplateTable, TournamentTable,
            SinglesParticipantTable,SinglesMatchTable, SinglesMatchLogTable,
            DoublesParticipantTable, DoublesMatchTable, DoublesMatchLogTable
        )


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
