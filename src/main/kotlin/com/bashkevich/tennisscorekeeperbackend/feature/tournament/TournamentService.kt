package com.bashkevich.tennisscorekeeperbackend.feature.tournament

import com.bashkevich.tennisscorekeeperbackend.feature.match.doubles.DoublesMatchRepository
import com.bashkevich.tennisscorekeeperbackend.feature.match.singles.SinglesMatchRepository
import com.bashkevich.tennisscorekeeperbackend.feature.participant.doubles.DoublesParticipantRepository
import com.bashkevich.tennisscorekeeperbackend.feature.participant.singles.SinglesParticipantRepository
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchStatus
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentDto
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentEntity
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentRequestDto
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentStatus
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentStatusBody
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentType
import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery
import com.bashkevich.tennisscorekeeperbackend.plugins.validateRequestConditions
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException

class TournamentService(
    private val tournamentRepository: TournamentRepository,
    private val singlesMatchRepository: SinglesMatchRepository,
    private val doublesMatchRepository: DoublesMatchRepository,
    private val singlesParticipantRepository: SinglesParticipantRepository,
    private val doublesParticipantRepository: DoublesParticipantRepository,
) {
    suspend fun getTournaments(): List<TournamentDto> {
        return dbQuery {
            tournamentRepository.getTournaments().map { it.toDto() }
        }
    }

    suspend fun addTournament(tournamentRequestDto: TournamentRequestDto): TournamentDto {
        return dbQuery {
            tournamentRepository.addTournament(tournamentRequestDto).toDto()
        }
    }

    suspend fun getTournamentById(tournamentId: Int): TournamentDto {
        return dbQuery {
            if (tournamentId == 0) throw BadRequestException("Wrong format of tournament id")

            tournamentRepository.getTournamentById(tournamentId)?.toDto()
                ?: throw NotFoundException("No tournament found by that id")
        }
    }

    suspend fun updateTournamentStatus(tournamentId: Int, tournamentStatusBody: TournamentStatusBody) {
        dbQuery {
            if (tournamentId == 0) throw BadRequestException("Wrong format of tournament id")

            val tournament = tournamentRepository.getTournamentById(tournamentId)
                ?: throw NotFoundException("No tournament found by that id")

            val currentStatus = tournament.status
            val newStatus = tournamentStatusBody.status

            validateRequestConditions {
                when {
                    currentStatus == newStatus -> ""
                    currentStatus == TournamentStatus.NOT_STARTED && newStatus == TournamentStatus.IN_PROGRESS -> ""
                    currentStatus == TournamentStatus.IN_PROGRESS && newStatus == TournamentStatus.COMPLETED -> {
                        val incompletedMatchesAmount = when (tournament.type) {
                            TournamentType.SINGLES -> singlesMatchRepository.getIncompletedMatchesAmount(tournamentId)
                            TournamentType.DOUBLES -> doublesMatchRepository.getIncompletedMatchesAmount(tournamentId)
                        }

                        if (incompletedMatchesAmount > 0) {
                            "Cannot make tournament completed. There are incompleted matches"
                        } else ""
                    }

                    else -> "Cannot update status from $currentStatus to $newStatus"
                }
            }


            tournamentRepository.updateStatus(tournamentId, newStatus)
        }
    }

    private fun TournamentEntity.toDto(): TournamentDto {
        val (participants, matches, uncompletedMatches) = when (type) {
            TournamentType.SINGLES -> {
                val m = singlesMatchRepository.getMatches(id.value)
                val p = singlesParticipantRepository.getParticipantsByTournament(id.value)
                Triple(p, m, m.count { it.status != MatchStatus.COMPLETED })
            }
            TournamentType.DOUBLES -> {
                val m = doublesMatchRepository.getMatches(id.value)
                val p = doublesParticipantRepository.getParticipantsByTournament(id.value)
                Triple(p, m, m.count { it.status != MatchStatus.COMPLETED })
            }
        }
        return TournamentDto(
            id = id.value.toString(),
            name = name,
            type = type,
            status = status,
            setsToWin = setsToWin,
            regularSetId = regularSetTemplate.id.value.toString(),
            decidingSetId = decidingSetTemplate.id.value.toString(),
            themeId = theme.id.value.toString(),
            totalParticipants = participants.size,
            totalMatches = matches.size,
            uncompletedMatches = uncompletedMatches,
        )
    }

}
