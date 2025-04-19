package com.bashkevich.tennisscorekeeperbackend.feature.match

import com.bashkevich.tennisscorekeeperbackend.feature.match.websocket.MatchObserver
import com.bashkevich.tennisscorekeeperbackend.feature.player.PlayerRepository
import com.bashkevich.tennisscorekeeperbackend.model.match.ChangeScoreBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchLogEvent
import com.bashkevich.tennisscorekeeperbackend.model.match.ScoreType
import com.bashkevich.tennisscorekeeperbackend.model.match.ServeBody
import com.bashkevich.tennisscorekeeperbackend.model.match.TennisGameDto
import com.bashkevich.tennisscorekeeperbackend.model.match.TennisSetDto
import com.bashkevich.tennisscorekeeperbackend.model.match.toTennisGameDto
import com.bashkevich.tennisscorekeeperbackend.model.match.toTennisSetDto
import com.bashkevich.tennisscorekeeperbackend.model.player.toPlayerInMatchDto
import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery
import com.bashkevich.tennisscorekeeperbackend.plugins.validateBody
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException

class MatchService(
    private val matchRepository: MatchRepository,
    private val matchLogRepository: MatchLogRepository,
    private val playerRepository: PlayerRepository,
) {
    suspend fun addMatch(matchBody: MatchBody) {
        return dbQuery {

            val firstPlayerId = matchBody.firstPlayerId

            val secondPlayerId = matchBody.secondPlayerId


//            validateBody(matchBody) {
//                val firstPlayer = playerRepository.getPlayerById(firstPlayerId)
//
//                val secondPlayer = playerRepository.getPlayerById(secondPlayerId)
//
//                firstPlayer != null && secondPlayer != null
//            }

            matchRepository.addMatch(firstPlayerId, secondPlayerId)
        }
    }

    suspend fun getMatchById(matchId: Int): MatchDto {
        return dbQuery {
            val matchDto = buildMatchById(matchId)

            matchDto
        }
    }

    suspend fun updateServe(matchId: Int, serveBody: ServeBody) {
        return dbQuery {
            if (matchId != 0) {
                val firstServePlayerId = serveBody.servingPlayerId

                validateBody(serveBody) {
                    val playerId = playerRepository.getPlayerById(firstServePlayerId)

                    playerId != null
                }
                val updatedRows = matchRepository.updateServe(matchId, firstServePlayerId)

                if (updatedRows < 1) throw NotFoundException("Match not found")

                val matchDto = buildMatchById(matchId)

                MatchObserver.notifyChange(matchDto)
            } else throw BadRequestException("Incorrect id")
        }
    }

    private fun buildMatchById(matchId: Int): MatchDto {

        if (matchId != 0) {
            val matchEntity = matchRepository.getMatchById(matchId)
                ?: throw NotFoundException("No match found!")

            val previousSets = matchLogRepository.getPreviousSets(matchId).map { it.toTennisSetDto() }

            val setNumber = previousSets.size + 1

            val lastPoint = matchLogRepository.getLastPoint(matchId)

            val currentServe = lastPoint?.currentServe ?: matchEntity.firstPlayerServe
            val firstPlayer = playerRepository.getPlayerById(matchEntity.firstPlayerId)
                ?.toPlayerInMatchDto(currentServe)
                ?: throw NotFoundException("Player with id = ${matchEntity.firstPlayerId} not found")

            val secondPlayer = playerRepository.getPlayerById(matchEntity.secondPlayerId)
                ?.toPlayerInMatchDto(currentServe)
                ?: throw NotFoundException("Player with id = ${matchEntity.secondPlayerId} not found")

            val lastGame = matchLogRepository.getCurrentSet(matchId,setNumber)

            val currentSet =
                if (lastPoint?.scoreType == ScoreType.SET) TennisSetDto(0, 0) else lastGame?.toTennisSetDto()
                    ?: TennisSetDto(0, 0)

            val currentGame = if (lastPoint?.scoreType in listOf(ScoreType.GAME, ScoreType.SET)) TennisGameDto(
                "0",
                "0"
            ) else lastPoint?.toTennisGameDto() ?: TennisGameDto("0", "0")

            val matchDto = MatchDto(
                matchId, firstPlayer, secondPlayer, previousSets, currentSet,
                currentGame
            )

            return matchDto
        } else throw BadRequestException("Incorrect id")
    }

    suspend fun updateScore(matchId: Int, changeScoreBody: ChangeScoreBody) {
        return dbQuery {
            if (matchId != 0) {
                val scoringPlayerId = changeScoreBody.playerId


                validateBody(changeScoreBody) {
                    val playerId = playerRepository.getPlayerById(scoringPlayerId)

                    playerId != null
                }

                val matchEntity = matchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")


                val lastPoint = matchLogRepository.getLastPoint(matchId)

                var setNumber = lastPoint?.setNumber ?: 1

                var pointNumber = lastPoint?.pointNumber ?: 0

                pointNumber++

                var currentServe = lastPoint?.currentServe ?: matchEntity.firstPlayerServe!!

                var firstPlayerPoints = 0
                var secondPlayerPoints = 0
                var scoreType = ScoreType.POINT


                if (lastPoint?.scoreType == ScoreType.SET){
                    setNumber++
                }

                if (lastPoint?.scoreType !in listOf(ScoreType.GAME, ScoreType.SET)) {
                    firstPlayerPoints = lastPoint?.firstPlayerPoints ?: 0
                    secondPlayerPoints = lastPoint?.secondPlayerPoints ?: 0
                }


                when {
                    matchEntity.firstPlayerId == scoringPlayerId -> firstPlayerPoints++
                    matchEntity.secondPlayerId == scoringPlayerId -> secondPlayerPoints++
                    else -> throw BadRequestException("Wrong player id in request")
                }

                val isFirstPlayerWonGame =
                    (firstPlayerPoints == 4 && secondPlayerPoints < 3) || (firstPlayerPoints > 4 && firstPlayerPoints - secondPlayerPoints == 2)
                val isSecondPlayerWonGame =
                    (secondPlayerPoints == 4 && firstPlayerPoints < 3) || (secondPlayerPoints > 4 && secondPlayerPoints - firstPlayerPoints == 2)


                if (isFirstPlayerWonGame || isSecondPlayerWonGame) {
                    scoreType = ScoreType.GAME
                    currentServe =
                        if (currentServe == matchEntity.firstPlayerId) matchEntity.secondPlayerId else matchEntity.firstPlayerId

                    val currentSet = matchLogRepository.getCurrentSet(matchId, setNumber)

                    firstPlayerPoints = currentSet?.firstPlayerPoints ?: 0
                    secondPlayerPoints = currentSet?.secondPlayerPoints ?: 0

                    if (isFirstPlayerWonGame) {
                        firstPlayerPoints++
                    } else {
                        secondPlayerPoints++
                    }

                    val isFirstPlayerWonSet =
                        (firstPlayerPoints == 6 && secondPlayerPoints < 5) || (firstPlayerPoints ==7 && secondPlayerPoints == 5)
                    val isSecondPlayerWonSet =
                        (secondPlayerPoints == 6 && firstPlayerPoints < 5) || (secondPlayerPoints ==7 && firstPlayerPoints == 5)

                    if (isFirstPlayerWonSet || isSecondPlayerWonSet){
                        scoreType = ScoreType.SET
                    }

                }

                val matchLogEvent = MatchLogEvent(
                    matchId = matchId,
                    setNumber = setNumber,
                    pointNumber = pointNumber,
                    scoreType = scoreType,
                    currentServe = currentServe,
                    firstPlayerPoints = firstPlayerPoints,
                    secondPlayerPoints = secondPlayerPoints
                )

                matchLogRepository.insertMatchLogEvent(matchLogEvent)

                val matchDto = buildMatchById(matchId)

                MatchObserver.notifyChange(matchDto)
            } else throw BadRequestException("Incorrect id")
        }
    }
}