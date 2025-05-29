package com.bashkevich.tennisscorekeeperbackend.feature.participant.singles

import com.bashkevich.tennisscorekeeperbackend.feature.player.PlayerRepository
import io.ktor.server.plugins.BadRequestException
import kotlinx.datetime.LocalDateTime
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.first
import org.jetbrains.kotlinx.dataframe.api.map
import org.jetbrains.kotlinx.dataframe.io.readExcel
import java.io.ByteArrayInputStream

class SinglesParticipantService(
    private val playerRepository: PlayerRepository
) {
    suspend fun uploadParticipants(tournamentId: Int, excelBytes: ByteArray) {
        val (numberOfSeededParticipants, participants) = parseParticipantListFromBytes(excelBytes)


    }

    private fun parseParticipantListFromBytes(excelBytes: ByteArray): Pair<Int, List<String>> {
        try {

            val seedDf = DataFrame.readExcel(ByteArrayInputStream(excelBytes), firstRowIsHeader = false).first()

            val numberOfSeededParticipantsSplit = seedDf[0].toString().split(":").map { it.trim() }

            val numberOfSeededParticipants =
                if (numberOfSeededParticipantsSplit.size > 1) numberOfSeededParticipantsSplit[1].toIntOrNull()
                    ?: 0 else throw Exception("There should be a delimiter of ':' to define number of seeded participants")

            val playersDf = DataFrame.readExcel(
                inputStream = ByteArrayInputStream(excelBytes),
                skipRows = 1,
                firstRowIsHeader = true
            )

            val participants = playersDf.map { row ->
                val name = row["Имя"]?.toString() ?: ""
                val surname = row["Фамилия"]?.toString() ?: ""
                val dateBirth = row["Дата рождения"]?.toString() ?: ""

                "$name $surname ${LocalDateTime.parse(dateBirth).date}"
            }

            return Pair(numberOfSeededParticipants, participants)

        } catch (e: Exception) {
            throw BadRequestException("Error processing Excel file: ${e.message}", e)
        }
    }
}