package com.bashkevich.tennisscorekeeperbackend.feature.participant

import com.bashkevich.tennisscorekeeperbackend.feature.participant.doubles.DoublesParticipantService
import com.bashkevich.tennisscorekeeperbackend.feature.participant.singles.SinglesParticipantService
import com.bashkevich.tennisscorekeeperbackend.feature.tournament.TournamentRepository
import com.bashkevich.tennisscorekeeperbackend.feature.tournament.api.LigaTennisaApiClient
import com.bashkevich.tennisscorekeeperbackend.feature.tournament.api.TournamentApiRelationRepository
import com.bashkevich.tennisscorekeeperbackend.model.participant.ParticipantDto
import com.bashkevich.tennisscorekeeperbackend.model.participant.ParticipantUploadRequestDto
import com.bashkevich.tennisscorekeeperbackend.model.player.api.LigaTennisaPlayerDto
import com.bashkevich.tennisscorekeeperbackend.model.player.api.LigaTennisaPlayersOrderDto
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentStatus
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentType
import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

class ParticipantServiceRouter(
    private val tournamentRepository: TournamentRepository,
    private val tournamentApiRelationRepository: TournamentApiRelationRepository,
    private val ligaTennisaApiClient: LigaTennisaApiClient,
    private val singlesParticipantService: SinglesParticipantService,
    private val doublesParticipantService: DoublesParticipantService,
) {

    suspend fun uploadParticipants(tournamentId: Int, uploadRequest: ParticipantUploadRequestDto): List<ParticipantDto> {
        // кривое значение seed_amount трактуется как 0: посевы не выставляются
        val seedAmount = (uploadRequest.seedAmount ?: 0).coerceAtLeast(0)

        val (tournamentType, tournamentApiId) = dbQuery {
            if (tournamentId == 0) throw BadRequestException("Wrong format of tournament id")

            val tournament = tournamentRepository.getTournamentById(tournamentId)
                ?: throw NotFoundException("No tournament found by that id")

            val apiId = tournamentApiRelationRepository.getApiIdByTournamentId(tournamentId)
                ?: throw BadRequestException("Tournament is not linked to the League of Tennis API")

            Pair(tournament.type, apiId)
        }

        // далее сеть: вызовы внешнего API держим вне транзакций БД
        val apiTournament = ligaTennisaApiClient.getTournaments()
            .firstOrNull { it.id.toString() == tournamentApiId }
            ?: throw BadRequestException("Tournament with api id $tournamentApiId is not found in the League of Tennis API")

        // пока на турнир в Лиге тенниса никто не зарегистрировался, players_order приходит null
        val playersOrder = apiTournament.playersOrder
            ?.takeIf { it.isNotBlank() }
            ?.let(::parsePlayersOrder)
            ?: throw BadRequestException("No players registered for this tournament in the League of Tennis API")

        // данные всех игроков из players_order: для одиночного — players, для парного — pairs
        // (у парного турнира players — это те же id, что и в pairs, дубли убирает distinct)
        val playersByApiId = fetchPlayersByIds(playersOrder.players + playersOrder.pairs.flatten())

        // после сетевых запросов проходит несколько секунд — статус турнира проверяется
        // в одном блоке непосредственно перед записью
        return dbQuery {
            val tournament = tournamentRepository.getTournamentById(tournamentId)
                ?: throw NotFoundException("No tournament found by that id")
            if (tournament.status == TournamentStatus.COMPLETED) {
                throw BadRequestException("Tournament is completed")
            }

            val participants = when (tournamentType) {
                TournamentType.SINGLES -> {
                    if (playersOrder.players.isEmpty()) {
                        throw BadRequestException("No players registered for this tournament in the League of Tennis API")
                    }

                    singlesParticipantService.uploadParticipants(
                        tournamentId,
                        seedAmount,
                        playersOrder.players,
                        playersByApiId
                    )
                }

                TournamentType.DOUBLES -> {
                    val apiPairs = playersOrder.pairs
                        .filter { it.size == 2 }
                        .map { Pair(it[0], it[1]) }

                    if (apiPairs.isEmpty()) {
                        throw BadRequestException("No players registered for this tournament in the League of Tennis API")
                    }

                    doublesParticipantService.uploadParticipants(tournamentId, seedAmount, apiPairs, playersByApiId)
                }
            }

            // количество сеяных берём из тела запроса (seed_amount), а не из фактического состава:
            // участников может быть меньше заявленного, остальные добавятся позже
            tournamentRepository.updateSeededParticipantsAmount(tournamentId, seedAmount)

            participants
        }
    }

    suspend fun getParticipantsByTournament(tournamentId: Int): List<ParticipantDto> {
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

    /** Данные игроков внешнего API загружаются параллельно. */
    private suspend fun fetchPlayersByIds(apiPlayerIds: List<Int>): Map<Int, LigaTennisaPlayerDto> {
        return coroutineScope {
            apiPlayerIds.distinct()
                .map { apiPlayerId -> async { ligaTennisaApiClient.getPlayerById(apiPlayerId) } }
                .awaitAll()
                .associateBy { it.id }
        }
    }

    private val apiPlayerIdsJson = Json { ignoreUnknownKeys = true }

    /** players_order приходит строкой, внутри которой JSON со списками игроков и пар. */
    private fun parsePlayersOrder(raw: String): LigaTennisaPlayersOrderDto {
        return runCatching { apiPlayerIdsJson.decodeFromString<LigaTennisaPlayersOrderDto>(raw) }
            .getOrElse { throw BadRequestException("Players order has invalid format in the League of Tennis API response") }
    }
}
