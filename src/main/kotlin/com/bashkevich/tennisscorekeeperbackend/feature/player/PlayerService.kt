package com.bashkevich.tennisscorekeeperbackend.feature.player

import com.bashkevich.tennisscorekeeperbackend.feature.player.api.PlayerApiRelationRepository
import com.bashkevich.tennisscorekeeperbackend.feature.tournament.api.LigaTennisaApiClient
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerBodyDto
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerDto
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerEntity
import com.bashkevich.tennisscorekeeperbackend.model.player.api.LigaTennisaPlayerDto
import com.bashkevich.tennisscorekeeperbackend.model.player.api.PlayerApiPrefillResultDto
import com.bashkevich.tennisscorekeeperbackend.model.player.api.resolveDateBirth
import com.bashkevich.tennisscorekeeperbackend.model.player.toDto
import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException

class PlayerService(
    private val playerRepository: PlayerRepository,
    private val playerApiRelationRepository: PlayerApiRelationRepository,
    private val ligaTennisaApiClient: LigaTennisaApiClient,
) {

    suspend fun addPlayer(playerBodyDto: PlayerBodyDto): PlayerDto {
        return dbQuery {
            playerRepository.addPlayer(
                playerSurname = playerBodyDto.surname,
                playerName = playerBodyDto.name,
                playerDateBirth = playerBodyDto.dateBirth
            ).toDto()
        }
    }

    suspend fun getPlayerById(id: Int): PlayerDto {
        return dbQuery {
            playerRepository.getPlayerById(id)?.toDto() ?: throw NotFoundException("Player not found")
        }
    }

    /**
     * Возвращает нашего игрока для игрока внешнего API Лиги тенниса: по связи из player_api_relation,
     * а при её отсутствии — создав нового игрока вместе со связью. Данные уже связанного игрока
     * обновляются по свежему ответу API.
     * Вызывается внутри транзакции вызывающего кода — свою не открывает.
     */
    suspend fun resolvePlayerByApiId(apiPlayer: LigaTennisaPlayerDto, linkedPlayerId: Int?): PlayerEntity {
        // в данных API встречаются лишние пробелы («Захаркевич ») — в нашу базу они не пишутся
        val surname = apiPlayer.lastName?.trim()
            ?: throw BadRequestException("Player with api id ${apiPlayer.id} has no last name in the League of Tennis API")
        val name = apiPlayer.firstName?.trim()
            ?: throw BadRequestException("Player with api id ${apiPlayer.id} has no first name in the League of Tennis API")
        val dateBirth = apiPlayer.resolveDateBirth()

        return if (linkedPlayerId != null) {
            // связь есть — игрок обновляется по свежим данным API; если строки player нет
            // (рассинхронизация), игрок пересоздаётся под той же связью
            playerRepository.updatePlayer(linkedPlayerId, surname, name, dateBirth)
                ?: playerRepository.addPlayer(playerSurname = surname, playerName = name, playerDateBirth = dateBirth)
        } else {
            val newPlayer = playerRepository.addPlayer(playerSurname = surname, playerName = name, playerDateBirth = dateBirth)
            playerApiRelationRepository.addRelation(playerId = newPlayer.id.value, apiId = apiPlayer.id)
            newPlayer
        }
    }

    /**
     * Предзаполнение player_api_relation: каждый игрок внешнего API сопоставляется с нашим игроком
     * по имени и фамилии (без регистра); при ровно одном кандидате создаётся связь.
     * Идемпотентно: уже связанные api_id и наши игроки пропускаются. Одноразовая операция.
     */
    suspend fun prefillApiRelations(): PlayerApiPrefillResultDto {
        // список игроков внешнего API получаем вне транзакции
        val apiPlayers = ligaTennisaApiClient.getAllPlayers()

        return dbQuery {
            val relations = playerApiRelationRepository.getRelations()
            val linkedApiIds = relations.map { it.first }.toSet()
            val linkedPlayerIds = relations.map { it.second }.toSet()

            val unlinkedPlayers = playerRepository.getPlayers()
                .filter { it.id.value !in linkedPlayerIds }
                .toMutableList()

            var createdRelations = 0
            var skippedNoMatch = 0
            var skippedAmbiguous = 0
            var alreadyLinked = 0

            apiPlayers.forEach { apiPlayer ->
                if (apiPlayer.id in linkedApiIds) {
                    alreadyLinked++
                    return@forEach
                }

                // в данных API встречаются лишние пробелы («Захаркевич ») — перед сравнением убираем с обеих сторон
                val name = apiPlayer.firstName?.trim()
                val surname = apiPlayer.lastName?.trim()
                if (name == null || surname == null) {
                    skippedNoMatch++
                    return@forEach
                }

                val candidates = unlinkedPlayers.filter {
                    it.name.trim().equals(name, ignoreCase = true) && it.surname.trim().equals(surname, ignoreCase = true)
                }

                when (candidates.size) {
                    0 -> skippedNoMatch++
                    1 -> {
                        val player = candidates.first()
                        playerApiRelationRepository.addRelation(playerId = player.id.value, apiId = apiPlayer.id)
                        // один наш игрок не может получить две связи
                        unlinkedPlayers.remove(player)
                        createdRelations++
                    }
                    else -> skippedAmbiguous++
                }
            }

            PlayerApiPrefillResultDto(
                createdRelations = createdRelations,
                skippedNoMatch = skippedNoMatch,
                skippedAmbiguous = skippedAmbiguous,
                alreadyLinked = alreadyLinked,
            )
        }
    }
}
