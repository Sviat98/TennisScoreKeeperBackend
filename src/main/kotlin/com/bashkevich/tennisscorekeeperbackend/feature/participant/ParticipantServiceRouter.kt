package com.bashkevich.tennisscorekeeperbackend.feature.participant

import com.bashkevich.tennisscorekeeperbackend.feature.participant.doubles.DoublesParticipantService
import com.bashkevich.tennisscorekeeperbackend.feature.participant.singles.SinglesParticipantService
import com.bashkevich.tennisscorekeeperbackend.feature.tournament.TournamentRepository
import com.bashkevich.tennisscorekeeperbackend.model.participant.ParticipantDto
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentType
import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery
import io.ktor.http.ContentType
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray

class ParticipantServiceRouter(
    private val tournamentRepository: TournamentRepository,
    private val singlesParticipantService: SinglesParticipantService,
    private val doublesParticipantService: DoublesParticipantService,
) {
    suspend fun uploadParticipants(tournamentId: Int, fileData: MultiPartData) : List<ParticipantDto>{
        return dbQuery {
            if (tournamentId == 0) throw BadRequestException("Wrong format of tournament id")

            val tournament = tournamentRepository.getTournamentById(tournamentId)
                ?: throw NotFoundException("No tournament found by that id")

            val excelBytes = loadExcelBytesFromMultipart(fileData)

            if (excelBytes == null)
                throw BadRequestException("No Excel file found in request or incorrect part name/content type. Please use 'excelFile' as field name and application/vnd.openxmlformats-officedocument.spreadsheetml.sheet as content type.")

            val participants = when (tournament.type) {
                TournamentType.SINGLES -> {
                    singlesParticipantService.uploadParticipants(tournamentId, excelBytes)
                }

                TournamentType.DOUBLES -> {
                    doublesParticipantService.uploadParticipants(tournamentId, excelBytes)
                }
            }

            participants
        }

    }

    suspend fun getParticipantsByTournament(tournamentId: Int) : List<ParticipantDto>{
        return dbQuery {
            if (tournamentId == 0) throw BadRequestException("Wrong format of tournament id")

            val tournament = tournamentRepository.getTournamentById(tournamentId)
                ?: throw NotFoundException("No tournament found by that id")

            val participants = when (tournament.type) {
                TournamentType.SINGLES -> {
                    singlesParticipantService.getParticipantsByTournament(tournamentId)
                }

                TournamentType.DOUBLES -> {
                    doublesParticipantService.getParticipantsByTournament(tournamentId)
                }
            }

            participants
        }
    }

    private suspend fun loadExcelBytesFromMultipart(fileData: MultiPartData): ByteArray? {
        var excelBytes: ByteArray? = null

        fileData.forEachPart { part ->
            when (part) {
                is PartData.FileItem -> {
                    // Проверяем, что это файл Excel
                    if (part.name == "excelFile" && part.contentType == ContentType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {

                        excelBytes = part.provider().readRemaining().readByteArray() // Read all bytes into memory
                    } else {
                        // Опционально: обработать другие типы файлов или поля
                        throw BadRequestException("Received unexpected file part: ${part.name} with content type ${part.contentType}")
                    }
                }

                else -> {} // Игнорируем другие типы частей (например, BinaryItem)
            }
            part.dispose() // Важно освободить ресурсы части
        }

        return excelBytes
    }



}