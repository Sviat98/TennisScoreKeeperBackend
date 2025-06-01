package com.bashkevich.tennisscorekeeperbackend.feature.participant.doubles

import com.bashkevich.tennisscorekeeperbackend.feature.player.PlayerRepository
import com.bashkevich.tennisscorekeeperbackend.model.participant.DoublesParticipantDto
import com.bashkevich.tennisscorekeeperbackend.model.participant.ParticipantDto
import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantExcel
import com.bashkevich.tennisscorekeeperbackend.model.participant.toPlayerInParticipantDto
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerExcel
import io.ktor.server.plugins.BadRequestException
import kotlinx.datetime.LocalDateTime
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.first
import org.jetbrains.kotlinx.dataframe.api.map
import org.jetbrains.kotlinx.dataframe.io.readExcel
import java.io.ByteArrayInputStream


class DoublesParticipantService(
    private val playerRepository: PlayerRepository,
    private val doublesParticipantRepository: DoublesParticipantRepository,
) {
    suspend fun uploadParticipants(tournamentId: Int, excelBytes: ByteArray): List<ParticipantDto> {
        val (numberOfSeededParticipants, participants) = parseParticipantListFromBytes(excelBytes)

        val registeredParticipants = mutableListOf<ParticipantDto>()

        participants.forEachIndexed { index, participant ->
            var seed: Int? = null

            // игрок, указанный в файле первым
            val firstPlayerInitial = playerRepository.searchPlayer(
                surname = participant.firstPlayer.surname,
                name = participant.firstPlayer.name,
                dateBirth = participant.firstPlayer.dateBirth
            )
                ?: playerRepository.addPlayer(
                    playerSurname = participant.firstPlayer.surname,
                    playerName = participant.firstPlayer.name,
                    playerDateBirth = participant.firstPlayer.dateBirth
                )

            // игрок, указанный в файле вторым
            val secondPlayerInitial = playerRepository.searchPlayer(
                surname = participant.secondPlayer.surname,
                name = participant.secondPlayer.name,
                dateBirth = participant.secondPlayer.dateBirth
            )
                ?: playerRepository.addPlayer(
                    playerSurname = participant.secondPlayer.surname,
                    playerName = participant.secondPlayer.name,
                    playerDateBirth = participant.secondPlayer.dateBirth
                )

            // если id первого меньше второго, то порядок отображения сохраняется, в противном случае мы его меняем
            val saveOrderAtDisplay = firstPlayerInitial.id.value < secondPlayerInitial.id.value

            val firstPlayerToInsert = if (saveOrderAtDisplay) firstPlayerInitial else secondPlayerInitial
            val secondPlayerToInsert = if (saveOrderAtDisplay) secondPlayerInitial else firstPlayerInitial


            if (index < numberOfSeededParticipants) {
                seed = index + 1
            }

            val participantId = doublesParticipantRepository.upsertParticipant(
                tournamentId = tournamentId,
                participantSeed = seed,
                firstPlayerId = firstPlayerToInsert.id.value,
                secondPlayerId = secondPlayerToInsert.id.value,
                displayOrderFlag = saveOrderAtDisplay
            )

            val registeredParticipant =
                DoublesParticipantDto(
                    id = participantId.toString(),
                    seed = seed,
                    firstPlayer = firstPlayerInitial.toPlayerInParticipantDto(),
                    secondPlayer = secondPlayerInitial.toPlayerInParticipantDto()
                )

            registeredParticipants.add(registeredParticipant)
        }

        return registeredParticipants
    }

    private fun parseParticipantListFromBytes(excelBytes: ByteArray): Pair<Int, List<DoublesParticipantExcel>> {
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
                val firstPlayerName = row[0]?.toString()?.trim() ?: ""
                val firstPlayerSurname = row[1]?.toString()?.trim() ?: ""
                val firstPlayerDateBirth = LocalDateTime.parse(row[2]?.toString()?.trim() ?: "").date

                val secondPlayerName = row[3]?.toString()?.trim() ?: ""
                val secondPlayerSurname = row[4]?.toString()?.trim() ?: ""
                val secondPlayerDateBirth = LocalDateTime.parse(row[5]?.toString()?.trim() ?: "").date

                val firstPlayerRating = (row[6] as? Double)?.toInt() ?: 0
                val secondPlayerRating = (row[7] as? Double)?.toInt() ?: 0


                DoublesParticipantExcel(
                    firstPlayer = PlayerExcel(
                        name = firstPlayerName,
                        surname = firstPlayerSurname,
                        dateBirth = firstPlayerDateBirth
                    ),
                    secondPlayer = PlayerExcel(
                        name = secondPlayerName,
                        surname = secondPlayerSurname,
                        dateBirth = secondPlayerDateBirth
                    ),

                    rating = firstPlayerRating + secondPlayerRating
                )
            }.sortedByDescending { it.rating }

            return Pair(numberOfSeededParticipants, participants)

        } catch (e: Exception) {
            throw BadRequestException("Error processing Excel file: ${e.message}", e)
        }
    }
}