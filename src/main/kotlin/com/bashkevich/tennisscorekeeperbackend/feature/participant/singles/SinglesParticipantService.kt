package com.bashkevich.tennisscorekeeperbackend.feature.participant.singles

import com.bashkevich.tennisscorekeeperbackend.feature.match.singles.SinglesMatchRepository
import com.bashkevich.tennisscorekeeperbackend.feature.player.PlayerService
import com.bashkevich.tennisscorekeeperbackend.feature.player.api.PlayerApiRelationRepository
import com.bashkevich.tennisscorekeeperbackend.model.participant.ParticipantDto
import com.bashkevich.tennisscorekeeperbackend.model.participant.SinglesParticipantDto
import com.bashkevich.tennisscorekeeperbackend.model.participant.api.SinglesParticipantApi
import com.bashkevich.tennisscorekeeperbackend.model.participant.toDto
import com.bashkevich.tennisscorekeeperbackend.model.player.api.LigaTennisaPlayerDto
import com.bashkevich.tennisscorekeeperbackend.model.player.toDto

class SinglesParticipantService(
    private val playerService: PlayerService,
    private val playerApiRelationRepository: PlayerApiRelationRepository,
    private val singlesParticipantRepository: SinglesParticipantRepository,
    private val singlesMatchRepository: SinglesMatchRepository,
) {

    fun getParticipantsByTournament(tournamentId: Int) : List<ParticipantDto> {
        val participants = singlesParticipantRepository.getParticipantsByTournament(tournamentId = tournamentId).map { it.toDto() }

        return participants
    }

    suspend fun uploadParticipants(
        tournamentId: Int,
        seedAmount: Int,
        apiPlayerIds: List<Int>,
        playersByApiId: Map<Int, LigaTennisaPlayerDto>,
    ): List<ParticipantDto> {
        val existingSeedByPlayerId = singlesParticipantRepository.getParticipantsByTournament(tournamentId)
            .associate { it.player.id.value to it.seed }

        // пока у турнира нет матчей, посев при каждой загрузке пересчитывается у всех заново
        val tournamentHasMatches = singlesMatchRepository.getMatches(tournamentId).isNotEmpty()
        // идущий турнир: новые участники занимают посевы после максимального занятого
        var nextSeed = existingSeedByPlayerId.values.filterNotNull().maxOrNull()?.plus(1) ?: 1

        val linkedPlayerIds = playerApiRelationRepository
            .getPlayerIdsByApiIds(playersByApiId.keys)
            .associate { it.first to it.second }

        val participants = apiPlayerIds
            .mapNotNull { apiPlayerId -> playersByApiId[apiPlayerId] }
            .map { apiPlayer ->
                SinglesParticipantApi(
                    player = playerService.resolvePlayerByApiId(apiPlayer, linkedPlayerIds[apiPlayer.id]),
                    rating = apiPlayer.eloPoints ?: 0,
                )
            }
            .sortedByDescending { it.rating }

        val registeredParticipants = mutableListOf<ParticipantDto>()
        val registeredParticipantIds = mutableListOf<Int>()

        participants.forEachIndexed { index, participant ->
            val currentSeed = existingSeedByPlayerId[participant.player.id.value]

            val seed = when {
                !tournamentHasMatches -> if (index < seedAmount) index + 1 else null
                // у уже добавленного участника посев сохраняется
                currentSeed != null -> currentSeed
                // новый участник идёт в конец: посев достаётся ему, только если свободные слоты ещё есть
                nextSeed <= seedAmount -> nextSeed++
                else -> null
            }

            val participantId = singlesParticipantRepository.upsertParticipant(
                tournamentId = tournamentId,
                participantSeed = seed,
                playerId = participant.player.id.value
            )

            registeredParticipantIds.add(participantId)

            val registeredParticipant = SinglesParticipantDto(participantId.toString(), seed, participant.player.toDto())

            registeredParticipants.add(registeredParticipant)
        }

        singlesParticipantRepository.deleteUnnecessaryParticipants(
            tournamentId = tournamentId,
            registeredParticipantIds = registeredParticipantIds.toList()
        )

        return registeredParticipants
    }
}
