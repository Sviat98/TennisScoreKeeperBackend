package com.bashkevich.tennisscorekeeperbackend.feature.match

import com.bashkevich.tennisscorekeeperbackend.feature.match.match_log.MatchLogRepository
import com.bashkevich.tennisscorekeeperbackend.feature.match.set_template.SetTemplateRepository
import com.bashkevich.tennisscorekeeperbackend.feature.match.websocket.MatchObserver
import com.bashkevich.tennisscorekeeperbackend.feature.player.PlayerRepository
import com.bashkevich.tennisscorekeeperbackend.model.match.ChangeScoreBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match_log.MatchLogEvent
import com.bashkevich.tennisscorekeeperbackend.model.match.ScoreType
import com.bashkevich.tennisscorekeeperbackend.model.match.ServeBody
import com.bashkevich.tennisscorekeeperbackend.model.match.TennisGameDto
import com.bashkevich.tennisscorekeeperbackend.model.match.TennisSetDto
import com.bashkevich.tennisscorekeeperbackend.model.match.toTennisGameDto
import com.bashkevich.tennisscorekeeperbackend.model.match.toTennisSetDto
import com.bashkevich.tennisscorekeeperbackend.model.player.toPlayerInMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.set_template.TiebreakMode
import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery
import com.bashkevich.tennisscorekeeperbackend.plugins.validateBody
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException

class MatchService(
    private val matchRepository: MatchRepository,
    private val matchLogRepository: MatchLogRepository,
    private val setTemplateRepository: SetTemplateRepository,
    private val playerRepository: PlayerRepository,
) {
    suspend fun addMatch(matchBody: MatchBody) {
        return dbQuery {


//            validateBody(matchBody) {
//                val firstPlayer = playerRepository.getPlayerById(firstPlayerId)
//
//                val secondPlayer = playerRepository.getPlayerById(secondPlayerId)
//
//                firstPlayer != null && secondPlayer != null
//            }

            matchRepository.addMatch(matchBody)
        }
    }

    suspend fun getMatchById(matchId: Int): MatchDto {
        return dbQuery {
            if (matchId != 0) {
                val matchEntity = matchRepository.getMatchById(matchId)
                    ?: throw NotFoundException("No match found!")

                val lastPointInTable = matchLogRepository.getLastPoint(matchId)

                val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + matchEntity.pointShift

                val matchDto = buildMatchById(matchId,lastPointNumber)

                matchDto
            }else throw BadRequestException("Incorrect id")
        }
    }

    suspend fun updateServe(matchId: Int, serveBody: ServeBody) {
        return dbQuery {
            if (matchId != 0) {
                val firstServePlayerId = serveBody.servingPlayerId.toIntOrNull() ?: 0

                validateBody(serveBody) {
                    val playerId = playerRepository.getPlayerById(firstServePlayerId)

                    playerId != null
                }
                val updatedRows = matchRepository.updateServe(matchId, firstServePlayerId)

                if (updatedRows < 1) throw NotFoundException("Match not found")

                val matchDto = buildMatchById(matchId,0)

                MatchObserver.notifyChange(matchDto)
            } else throw BadRequestException("Incorrect id")
        }
    }

    private fun buildMatchById(matchId: Int, lastPointNumber: Int): MatchDto {
            val matchEntity = matchRepository.getMatchById(matchId)
                ?: throw NotFoundException("No match found!")

            val previousSets = matchLogRepository.getPreviousSets(matchId,lastPointNumber).map { it.toTennisSetDto() }

            val setNumber = previousSets.size + 1

            val lastPoint = matchLogRepository.getLastPoint(matchId,lastPointNumber)

            val currentServe = lastPoint?.currentServe ?: matchEntity.firstPlayerServe
            val firstPlayer = playerRepository.getPlayerById(matchEntity.firstPlayerId)
                ?.toPlayerInMatchDto(currentServe)
                ?: throw NotFoundException("Player with id = ${matchEntity.firstPlayerId} not found")

            val secondPlayer = playerRepository.getPlayerById(matchEntity.secondPlayerId)
                ?.toPlayerInMatchDto(currentServe)
                ?: throw NotFoundException("Player with id = ${matchEntity.secondPlayerId} not found")

            val lastGame = matchLogRepository.getCurrentSet(matchId, setNumber, lastPointNumber)

            val currentSet =
                if (lastPoint?.scoreType == ScoreType.SET) TennisSetDto(0, 0) else lastGame?.toTennisSetDto()
                    ?: TennisSetDto(0, 0)

            val currentGame = if (lastPoint?.scoreType in listOf(ScoreType.GAME, ScoreType.SET)) TennisGameDto(
                "0",
                "0"
            ) else lastPoint?.toTennisGameDto() ?: TennisGameDto("0", "0")

            val matchDto = MatchDto(
                id = matchId.toString(),
                pointShift = matchEntity.pointShift,
                firstPlayer = firstPlayer,
                secondPlayer = secondPlayer,
                previousSets = previousSets,
                currentSet = currentSet,
                currentGame = currentGame
            )

            return matchDto
    }

    suspend fun updateScore(matchId: Int, changeScoreBody: ChangeScoreBody) {
        return dbQuery {
            if (matchId != 0) {
                val scoringPlayerId = changeScoreBody.playerId.toIntOrNull() ?: 0


                validateBody(changeScoreBody) {
                    val playerId = playerRepository.getPlayerById(scoringPlayerId)

                    playerId != null
                }

                val matchEntity = matchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

                val lastPointInTable = matchLogRepository.getLastPoint(matchId)

                val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + matchEntity.pointShift

                val lastPoint = matchLogRepository.getLastPoint(matchId,lastPointNumber)

                if (matchEntity.pointShift<0){
                    matchRepository.updatePointShift(matchId = matchId, newPointShift = 0)

                    matchLogRepository.removeEvents(matchId = matchId,pointNumber = lastPointNumber)
                }

                val setsToWin = matchEntity.setsToWin

                val firstPlayerId = matchEntity.firstPlayerId

                val secondPlayerId = matchEntity.secondPlayerId

                var setNumber = lastPoint?.setNumber ?: 1

                val isDecidingSet = setNumber == 2 * setsToWin - 1

                val currentSetTemplate = if (isDecidingSet) {
                    setTemplateRepository.getSetTemplateById(matchEntity.decidingSet)
                } else {
                    setTemplateRepository.getSetTemplateById(matchEntity.regularSet)
                } ?: throw NotFoundException("No set template found!")

                var pointNumber = lastPoint?.pointNumber ?: 0

                pointNumber++

                var currentServe = lastPoint?.currentServe ?: matchEntity.firstPlayerServe!!

                var firstPlayerPoints = 0
                var secondPlayerPoints = 0
                var scoreType: ScoreType

                if (changeScoreBody.scoreType == ScoreType.GAME) {
                    if (lastPoint?.scoreType == ScoreType.POINT) throw BadRequestException("Cannot add game")
                    scoreType = ScoreType.GAME
                } else {
                    scoreType = ScoreType.POINT
                }


                if (lastPoint?.scoreType == ScoreType.SET) {
                    setNumber++
                }

                if (lastPoint?.scoreType !in listOf(ScoreType.GAME, ScoreType.SET)) {
                    firstPlayerPoints = lastPoint?.firstPlayerPoints ?: 0
                    secondPlayerPoints = lastPoint?.secondPlayerPoints ?: 0
                }


                when {
                    firstPlayerId == scoringPlayerId -> firstPlayerPoints++
                    secondPlayerId == scoringPlayerId -> secondPlayerPoints++
                    else -> throw BadRequestException("Wrong player id in request")
                }

                val currentSet = matchLogRepository.getCurrentSet(matchId, setNumber, lastPointNumber)

                var isFirstPlayerWonGame: Boolean
                var isSecondPlayerWonGame: Boolean

                val isTiebreakMode = when (currentSetTemplate.tiebreakMode) {
                    TiebreakMode.EARLY -> currentSet?.firstPlayerPoints == currentSet?.secondPlayerPoints && currentSet?.firstPlayerPoints == currentSetTemplate.gamesToWin - 1
                    TiebreakMode.LATE -> currentSet?.firstPlayerPoints == currentSet?.secondPlayerPoints && currentSet?.firstPlayerPoints == currentSetTemplate.gamesToWin
                    else -> false
                }

                if (isTiebreakMode) {
                    val tiebreakPointsToWin = currentSetTemplate.tiebreakPointsToWin

                    isFirstPlayerWonGame =
                        firstPlayerPoints == tiebreakPointsToWin || (firstPlayerPoints > tiebreakPointsToWin && firstPlayerPoints - secondPlayerPoints == 2)
                    isSecondPlayerWonGame =
                        secondPlayerPoints == tiebreakPointsToWin || (secondPlayerPoints > tiebreakPointsToWin && secondPlayerPoints - firstPlayerPoints == 2)
                    scoreType = ScoreType.TIEBREAK_POINT

                    currentServe = when {
                        (firstPlayerPoints + secondPlayerPoints % 2 == 1 && currentSet?.currentServe == firstPlayerId) -> secondPlayerId
                        (firstPlayerPoints + secondPlayerPoints % 2 == 1 && currentSet?.currentServe == secondPlayerId) -> firstPlayerId
                        else -> lastPoint?.currentServe ?: matchEntity.firstPlayerServe!!
                    }
                } else {
                    isFirstPlayerWonGame = when {
                        changeScoreBody.scoreType == ScoreType.GAME && changeScoreBody.playerId == matchEntity.firstPlayerId.toString() -> true
                        currentSetTemplate.decidingPoint -> firstPlayerPoints == 4
                        else -> (firstPlayerPoints == 4 && secondPlayerPoints < 3) || (firstPlayerPoints > 4 && firstPlayerPoints - secondPlayerPoints == 2)
                    }

                    isSecondPlayerWonGame = when {
                        changeScoreBody.scoreType == ScoreType.GAME && changeScoreBody.playerId == matchEntity.secondPlayerId.toString() -> true
                        currentSetTemplate.decidingPoint -> secondPlayerPoints == 4
                        else -> (secondPlayerPoints == 4 && firstPlayerPoints < 3) || (secondPlayerPoints > 4 && secondPlayerPoints - firstPlayerPoints == 2)
                    }
                }


                if (isFirstPlayerWonGame || isSecondPlayerWonGame) {
                    scoreType = ScoreType.GAME
                    currentServe =
                        if (currentServe == matchEntity.firstPlayerId) matchEntity.secondPlayerId else matchEntity.firstPlayerId


                    firstPlayerPoints = currentSet?.firstPlayerPoints ?: 0
                    secondPlayerPoints = currentSet?.secondPlayerPoints ?: 0

                    if (isFirstPlayerWonGame) {
                        firstPlayerPoints++
                    } else {
                        secondPlayerPoints++
                    }

                    val gamesToWin = currentSetTemplate.gamesToWin

                    val isFirstPlayerWonSet = when {
                        isTiebreakMode && isFirstPlayerWonGame -> true
                        else -> (firstPlayerPoints == gamesToWin && secondPlayerPoints < gamesToWin - 1) || (firstPlayerPoints > gamesToWin && firstPlayerPoints - secondPlayerPoints == 2)
                    }

                    val isSecondPlayerWonSet = when {
                        isTiebreakMode && isSecondPlayerWonGame -> true
                        else -> (secondPlayerPoints == gamesToWin && firstPlayerPoints < gamesToWin - 1) || (secondPlayerPoints > gamesToWin && secondPlayerPoints - firstPlayerPoints == 2)
                    }

                    if (isFirstPlayerWonSet || isSecondPlayerWonSet) {
                        scoreType = ScoreType.SET
                        if (isTiebreakMode) {
                            currentServe = currentSet?.currentServe ?: matchEntity.firstPlayerServe!!
                        }
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

                val newLastPointNumber = lastPointNumber+1

                val matchDto = buildMatchById(matchId, newLastPointNumber)

                MatchObserver.notifyChange(matchDto)
            } else throw BadRequestException("Incorrect id")
        }
    }

    suspend fun undoPoint(matchId: Int) {
        return dbQuery {
            if (matchId != 0) {

                val matchEntity = matchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

                val lastPointInTable = matchLogRepository.getLastPoint(matchId)

                val pointShift = matchEntity.pointShift

                val newPointShift =pointShift-1

                val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + newPointShift

                if (lastPointNumber==0) throw BadRequestException("Cannot undo the point")

                matchRepository.updatePointShift(matchId =matchId, newPointShift = newPointShift)

                val matchDto = buildMatchById(matchId, lastPointNumber)

                MatchObserver.notifyChange(matchDto)
            } else throw BadRequestException("Incorrect id")
        }
    }

    suspend fun redoPoint(matchId: Int) {
        return dbQuery {
            if (matchId != 0) {

                val matchEntity = matchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

                val lastPointInTable = matchLogRepository.getLastPoint(matchId)

                val pointShift = matchEntity.pointShift

                if (pointShift==0) throw BadRequestException("Cannot redo the point")

                val newPointShift =pointShift+1

                val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + newPointShift

                matchRepository.updatePointShift(matchId =matchId, newPointShift = newPointShift)

                val matchDto = buildMatchById(matchId, lastPointNumber)

                MatchObserver.notifyChange(matchDto)
            } else throw BadRequestException("Incorrect id")
        }
    }
}