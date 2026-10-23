package com.bashkevich.tennisscorekeeperbackend.feature.participant.doubles

import com.bashkevich.tennisscorekeeperbackend.feature.match.doubles.DoublesMatchRepository
import com.bashkevich.tennisscorekeeperbackend.feature.player.PlayerService
import com.bashkevich.tennisscorekeeperbackend.feature.player.api.PlayerApiRelationRepository
import com.bashkevich.tennisscorekeeperbackend.model.participant.DoublesParticipantDto
import com.bashkevich.tennisscorekeeperbackend.model.participant.ParticipantDto
import com.bashkevich.tennisscorekeeperbackend.model.participant.api.DoublesParticipantApi
import com.bashkevich.tennisscorekeeperbackend.model.participant.toDto
import com.bashkevich.tennisscorekeeperbackend.model.player.api.LigaTennisaPlayerDto
import com.bashkevich.tennisscorekeeperbackend.model.player.toDto

class DoublesParticipantService(
    private val playerService: PlayerService,
    private val playerApiRelationRepository: PlayerApiRelationRepository,
    private val doublesParticipantRepository: DoublesParticipantRepository,
    private val doublesMatchRepository: DoublesMatchRepository,
) {
    fun getParticipantsByTournament(tournamentId: Int) : List<ParticipantDto> {
        val participants = doublesParticipantRepository.getParticipantsByTournament(tournamentId = tournamentId).map { it.toDto() }

        println(participants)
        return participants
    }

    suspend fun uploadParticipants(
        tournamentId: Int,
        seedAmount: Int,
        apiPairs: List<Pair<Int, Int>>,
        playersByApiId: Map<Int, LigaTennisaPlayerDto>,
    ): List<ParticipantDto> {
        // пара в БД хранится по возрастанию id игроков — для сверки с уже добавленными ключ тоже нормализуем
        val existingSeedByPlayerIds = doublesParticipantRepository.getParticipantsByTournament(tournamentId)
            .associate {
                Pair(
                    minOf(it.firstPlayer.id.value, it.secondPlayer.id.value),
                    maxOf(it.firstPlayer.id.value, it.secondPlayer.id.value),
                ) to it.seed
            }

        // пока у турнира нет матчей, посев при каждой загрузке пересчитывается у всех заново
        val tournamentHasMatches = doublesMatchRepository.getMatches(tournamentId).isNotEmpty()
        // идущий турнир: новые участники занимают посевы после максимального занятого
        var nextSeed = existingSeedByPlayerIds.values.filterNotNull().maxOrNull()?.plus(1) ?: 1

        val linkedPlayerIds = playerApiRelationRepository
            .getPlayerIdsByApiIds(playersByApiId.keys)
            .associate { it.first to it.second }

        val pairs = apiPairs
            .mapNotNull { (firstApiId, secondApiId) ->
                val firstApiPlayer = playersByApiId[firstApiId] ?: return@mapNotNull null
                val secondApiPlayer = playersByApiId[secondApiId] ?: return@mapNotNull null

                DoublesParticipantApi(
                    firstPlayer = playerService.resolvePlayerByApiId(firstApiPlayer, linkedPlayerIds[firstApiPlayer.id]),
                    secondPlayer = playerService.resolvePlayerByApiId(secondApiPlayer, linkedPlayerIds[secondApiPlayer.id]),
                    // рейтинг пары — сумма эло обоих игроков
                    rating = (firstApiPlayer.doublesEloPoints ?: 0) + (secondApiPlayer.doublesEloPoints ?: 0),
                )
            }
            .sortedByDescending { it.rating }

        val registeredParticipants = mutableListOf<ParticipantDto>()
        val registeredParticipantIds = mutableListOf<Int>()

        pairs.forEachIndexed { index, pair ->
            val firstPlayerId = pair.firstPlayer.id.value
            val secondPlayerId = pair.secondPlayer.id.value

            val currentSeed = existingSeedByPlayerIds[
                Pair(minOf(firstPlayerId, secondPlayerId), maxOf(firstPlayerId, secondPlayerId))
            ]

            val seed = when {
                !tournamentHasMatches -> if (index < seedAmount) index + 1 else null
                // у уже добавленной пары посев сохраняется
                currentSeed != null -> currentSeed
                // новая пара идёт в конец: посев достаётся ей, только если свободные слоты ещё есть
                nextSeed <= seedAmount -> nextSeed++
                else -> null
            }

            // если id первого меньше второго, то порядок отображения сохраняется, в противном случае мы его меняем
            val saveOrderAtDisplay = firstPlayerId < secondPlayerId

            val firstPlayerToInsert = if (saveOrderAtDisplay) pair.firstPlayer else pair.secondPlayer
            val secondPlayerToInsert = if (saveOrderAtDisplay) pair.secondPlayer else pair.firstPlayer

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
                    firstPlayer = pair.firstPlayer.toDto(),
                    secondPlayer = pair.secondPlayer.toDto()
                )
            registeredParticipantIds.add(participantId)
            registeredParticipants.add(registeredParticipant)
        }

        doublesParticipantRepository.deleteUnnecessaryParticipants(
            tournamentId = tournamentId,
            registeredParticipantIds = registeredParticipantIds.toList()
        )

        return registeredParticipants
    }
}
