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
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Schema
import  org.jetbrains.exposed.v1.core.Sequence;
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.addLogger
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

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

//        exec("CREATE UNIQUE INDEX idx_singles_participant_tournament_player \n" +
//                "ON singles_participant (tournament_id, player_id);")

        // Создание таблицы в базе данных
        //SchemaUtils.create(DoublesParticipantTestTable)
//
//        println("--- Тестирование upsert с нормализацией порядка ---")
//
//        // Функция для выполнения upsert с нормализацией ID игроков
//        fun upsertDoublesParticipant(
//            tournamentId: Int,
//            player1: Int,
//            player2: Int,
//            value: String
//        ) {
//            // Нормализация ID игроков: всегда меньший в firstPlayer, больший в secondPlayer
//            val normalizedFirstPlayer = minOf(player1, player2)
//            val normalizedSecondPlayer = maxOf(player1, player2)
//
//            try {
//                val result = DoublesParticipantTestTable.upsert(
//                    // Явно указываем колонки, по которым Exposed должен определять конфликт.
//                    // Это соответствует нашему уникальному индексу.
//                    DoublesParticipantTestTable.tournament,
//                    DoublesParticipantTestTable.firstPlayer,
//                    DoublesParticipantTestTable.secondPlayer,
//                    onUpdate = { updateStatement ->
//                        // Что делать при обновлении: обновить randomValue
//                        updateStatement[randomValue] = value
//                        println("Обновление существующей записи: tournament=$tournamentId, players=($player1, $player2), randomValue=$value")
//                    }
//                ) {
//                    // Значения для вставки или обновления
//                    it[tournament] = tournamentId
//                    it[firstPlayer] = normalizedFirstPlayer
//                    it[secondPlayer] = normalizedSecondPlayer
//                    it[randomValue] = value
//                    println("Попытка вставки/обновления: tournament=$tournamentId, players=($player1, $player2), randomValue=$value (нормализовано: $normalizedFirstPlayer, $normalizedSecondPlayer)")
//                }
//                if (result.insertedCount > 0) {
//                    println("Успешно вставлена новая запись.")
//                } else if (result.insertedCount > 0) {
//                    println("Успешно обновлена существующая запись.")
//                }
//
//            } catch (e: ExposedSQLException) {
//                // Обработка исключений, если что-то пошло не так (например, другой уникальный индекс)
//                println("Ошибка Exposed при upsert: ${e.message}")
//            } catch (e: Exception) {
//                println("Неизвестная ошибка при upsert: ${e.message}")
//            }
//            println("------------------------------------")
//        }
//
//        // 1. Вставка первой записи (1, 101, 102)
//        upsertDoublesParticipant(1, 101, 102, "Initial Value A")
//        // Ожидаемый результат: Вставка новой записи
//
//        // 2. Попытка вставки зеркальной записи (1, 102, 101)
//        // Она будет нормализована в (1, 101, 102) и вызовет конфликт.
//        upsertDoublesParticipant(1, 102, 101, "Updated Value B")
//        // Ожидаемый результат: Обновление существующей записи (randomValue изменится)
//
//        // 3. Вставка новой записи с другим турниром (2, 101, 102)
//        upsertDoublesParticipant(2, 101, 102, "New Tournament Value C")
//        // Ожидаемый результат: Вставка новой записи
//
//        // 4. Попытка вставки записи с другим игроком (1, 101, 103)
//        upsertDoublesParticipant(1, 101, 103, "Different Player Value D")
//        // Ожидаемый результат: Вставка новой записи
//
//        // Вывод всех записей для проверки
//        println("\n--- Все записи в таблице ---")
//        DoublesParticipantTestTable.selectAll().forEach {
//            println("ID: ${it[DoublesParticipantTestTable.id].value}, Tournament: ${it[DoublesParticipantTestTable.tournament]}, " +
//                    "Player1: ${it[DoublesParticipantTestTable.firstPlayer]}, Player2: ${it[DoublesParticipantTestTable.secondPlayer]}, " +
//                    "RandomValue: ${it[DoublesParticipantTestTable.randomValue]}")
//        }
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
        newSuspendedTransaction (Dispatchers.IO) {
            exec("SELECT 1")
            true
        }
    } catch (e: Exception) {
        false
    }
}

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction (Dispatchers.IO) {
        addLogger(StdOutSqlLogger)
        block()
    }
