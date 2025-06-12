package com.bashkevich.tennisscorekeeperbackend.feature.participant.singles

import com.bashkevich.tennisscorekeeperbackend.feature.player.PlayerRepository
import com.bashkevich.tennisscorekeeperbackend.model.participant.ParticipantDto
import com.bashkevich.tennisscorekeeperbackend.model.participant.SinglesParticipantDto
import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantExcel
import com.bashkevich.tennisscorekeeperbackend.model.participant.toDto
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerExcel
import com.bashkevich.tennisscorekeeperbackend.model.player.toPlayerInParticipantDto
import io.ktor.server.plugins.BadRequestException
import kotlinx.datetime.LocalDateTime
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.first
import org.jetbrains.kotlinx.dataframe.api.map
import org.jetbrains.kotlinx.dataframe.io.readExcel
import java.io.ByteArrayInputStream

class SinglesParticipantService(
    private val playerRepository: PlayerRepository,
    private val singlesParticipantRepository: SinglesParticipantRepository,
) {

    fun getParticipantsByTournament(tournamentId: Int) : List<ParticipantDto> {
        val participants = singlesParticipantRepository.getParticipantsByTournament(tournamentId = tournamentId).map { it.toDto() }

        return participants
    }

    suspend fun uploadParticipants(tournamentId: Int, excelBytes: ByteArray): List<ParticipantDto> {
        val (numberOfSeededParticipants, participants) = parseParticipantListFromBytes(excelBytes)

        val registeredParticipants = mutableListOf<ParticipantDto>()
        val registeredParticipantIds = mutableListOf<Int>()


        participants.forEachIndexed { index, participant ->
            var seed: Int? = null

            val player = playerRepository.searchPlayer(
                surname = participant.player.surname,
                name = participant.player.name,
                dateBirth = participant.player.dateBirth
            )
                ?: playerRepository.addPlayer(
                    playerSurname = participant.player.surname,
                    playerName = participant.player.name,
                    playerDateBirth = participant.player.dateBirth
                )

            if (index < numberOfSeededParticipants) {
                seed = index + 1
            }

            val participantId = singlesParticipantRepository.upsertParticipant(tournamentId = tournamentId,participantSeed = seed, playerId = player.id.value)

            registeredParticipantIds.add(participantId)

            val registeredParticipant = SinglesParticipantDto(participantId.toString(),seed, player.toPlayerInParticipantDto())

            registeredParticipants.add(registeredParticipant)
        }

        singlesParticipantRepository.deleteUnnecessaryParticipants(
            tournamentId = tournamentId,
            registeredParticipantIds = registeredParticipantIds.toList()
        )

        return registeredParticipants
    }

    private fun parseParticipantListFromBytes(excelBytes: ByteArray): Pair<Int, List<SinglesParticipantExcel>> {
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
                val name = row[0]?.toString()?.trim() ?: ""
                val surname = row[1]?.toString()?.trim() ?: ""
                val dateBirth = LocalDateTime.parse(row[2]?.toString()?.trim() ?: "").date
                val rating = (row[3] as? Double)?.toInt() ?: 0


                SinglesParticipantExcel(
                    player = PlayerExcel(name = name, surname = surname, dateBirth = dateBirth),
                    rating = rating
                )
            }.sortedByDescending { it.rating }

            return Pair(numberOfSeededParticipants, participants)

        } catch (e: Exception) {
            throw BadRequestException("Error processing Excel file: ${e.message}", e)
        }
    }
}