package com.bashkevich.tennisscorekeeperbackend.feature.match

import com.bashkevich.tennisscorekeeperbackend.feature.match.match_log.MatchLogRepository
import com.bashkevich.tennisscorekeeperbackend.feature.match.set_template.SetTemplateRepository
import com.bashkevich.tennisscorekeeperbackend.feature.match.websocket.MatchObserver
import com.bashkevich.tennisscorekeeperbackend.feature.player.PlayerRepository
import com.bashkevich.tennisscorekeeperbackend.model.match.ChangeScoreBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.match_log.MatchLogEvent
import com.bashkevich.tennisscorekeeperbackend.model.match.ScoreType
import com.bashkevich.tennisscorekeeperbackend.model.match.ServeBody
import com.bashkevich.tennisscorekeeperbackend.model.match.ShortMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.SpecialSetMode
import com.bashkevich.tennisscorekeeperbackend.model.match.TennisGameDto
import com.bashkevich.tennisscorekeeperbackend.model.match.TennisSetDto
import com.bashkevich.tennisscorekeeperbackend.model.match.toDto
import com.bashkevich.tennisscorekeeperbackend.model.match.toTennisGameDto
import com.bashkevich.tennisscorekeeperbackend.model.match.toTennisSetDto
import com.bashkevich.tennisscorekeeperbackend.model.player.toPlayerInMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateEntity
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
    suspend fun addMatch(matchBody: MatchBody): ShortMatchDto {
        return dbQuery {

//            validateBody(matchBody) {
//                val firstPlayer = playerRepository.getPlayerById(firstPlayerId)
//
//                val secondPlayer = playerRepository.getPlayerById(secondPlayerId)
//
//                firstPlayer != null && secondPlayer != null
//            }

            val newMatchId = matchRepository.addMatch(matchBody).value

            val shortMatchDto = matchRepository.getShortMatchById(newMatchId)

            shortMatchDto!!.toDto()
        }
    }

    suspend fun getMatches(): List<ShortMatchDto> {
        return dbQuery {
            matchRepository.getMatches().map { shortMatch ->
                shortMatch.toDto()
            }
        }
    }

    suspend fun getMatchById(matchId: Int): MatchDto {
        return dbQuery {
            if (matchId != 0) {
                val matchEntity = matchRepository.getMatchById(matchId)
                    ?: throw NotFoundException("No match found!")

                val lastPointInTable = matchLogRepository.getLastPoint(matchId)

                val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + matchEntity.pointShift

                val matchDto = buildMatchById(matchId, lastPointNumber)

                matchDto
            } else throw BadRequestException("Incorrect id")
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

                val matchDto = buildMatchById(matchId, 0)

                MatchObserver.notifyChange(matchDto)
            } else throw BadRequestException("Incorrect id")
        }
    }

    private fun buildMatchById(matchId: Int, lastPointNumber: Int): MatchDto {
        val matchEntity = matchRepository.getMatchById(matchId)
            ?: throw NotFoundException("No match found!")

        // для предыдущих партий всгде возвращаем ORDINARY,здесь нам нужен только сам счет
        val previousSets = matchLogRepository.getPreviousSets(matchId, lastPointNumber).map { it.toTennisSetDto() }

        val setNumber = previousSets.size + 1

        val lastPoint = matchLogRepository.getLastPoint(matchId, lastPointNumber)

        val setsToWin = matchEntity.setsToWin

        val currentSetTemplate = findSetTemplate(matchEntity, setNumber, setsToWin)

        val currentSetMode = calculateCurrentSetMode(currentSetTemplate)

        val firstPlayerId = matchEntity.firstPlayer.id
        val secondPlayerId = matchEntity.secondPlayer.id


        val currentServe = lastPoint?.currentServe ?: matchEntity.firstPlayerServe
        val winnerPlayerId = matchEntity.winner
        val firstPlayer = playerRepository.getPlayerById(firstPlayerId)
            ?.toPlayerInMatchDto(servingPlayerId = currentServe, winnerPlayerId = winnerPlayerId)
            ?: throw NotFoundException("Player with id = $firstPlayerId not found")

        val secondPlayer = playerRepository.getPlayerById(secondPlayerId)
            ?.toPlayerInMatchDto(servingPlayerId = currentServe, winnerPlayerId = winnerPlayerId)
            ?: throw NotFoundException("Player with id = $secondPlayerId not found")

        val lastGame = matchLogRepository.getCurrentSet(matchId, setNumber, lastPointNumber)

        val currentSet = when {
            lastPoint?.scoreType == ScoreType.SET -> TennisSetDto(0, 0, currentSetMode)
            currentSetMode == SpecialSetMode.SUPER_TIEBREAK -> TennisSetDto(
                firstPlayerGames = lastPoint?.firstPlayerPoints ?: 0, lastPoint?.secondPlayerPoints ?: 0, currentSetMode
            )

            else -> lastGame?.toTennisSetDto(
                specialSetMode = currentSetMode
            )
                ?: TennisSetDto(firstPlayerGames = 0, secondPlayerGames = 0, specialSetMode = currentSetMode)
        }

        val currentGame = when{
            currentSetMode== SpecialSetMode.SUPER_TIEBREAK -> TennisGameDto("0", "0")
            lastPoint?.scoreType in listOf(ScoreType.GAME, ScoreType.SET) -> TennisGameDto("0", "0")
            else -> lastPoint?.toTennisGameDto() ?: TennisGameDto("0", "0")
        }

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

                val lastPoint = matchLogRepository.getLastPoint(matchId, lastPointNumber)

                if (matchEntity.pointShift < 0) {
                    matchRepository.updatePointShift(matchId = matchId, newPointShift = 0)

                    matchLogRepository.removeEvents(matchId = matchId, pointNumber = lastPointNumber)
                }

                val setsToWin = matchEntity.setsToWin

                val firstPlayerId = matchEntity.firstPlayer.id

                val secondPlayerId = matchEntity.secondPlayer.id

                var setNumber = lastPoint?.setNumber ?: 1

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

                val currentSetTemplate = findSetTemplate(matchEntity, setNumber, setsToWin)

                val currentSetMode = calculateCurrentSetMode(currentSetTemplate)

                val currentSet = matchLogRepository.getCurrentSet(matchId, setNumber, lastPointNumber)

                val currentSetFirstPlayerPoints = currentSet?.firstPlayerPoints ?: 0
                val currentSetSecondPlayerPoints = currentSet?.secondPlayerPoints ?: 0

                var isFirstPlayerWonGame: Boolean
                var isSecondPlayerWonGame: Boolean

                val isTiebreakMode = when (currentSetTemplate.tiebreakMode) {
                    // пример для сета до 6 геймов
                    // EARLY - когда тайбрейк играется при сете 5-5
                    //LATE - когда тайбрейк играется при сете 6-6
                    TiebreakMode.EARLY -> currentSetFirstPlayerPoints == currentSetSecondPlayerPoints
                         && currentSetFirstPlayerPoints == currentSetTemplate.gamesToWin - 1
                    TiebreakMode.LATE -> currentSetFirstPlayerPoints == currentSetSecondPlayerPoints && currentSetFirstPlayerPoints == currentSetTemplate.gamesToWin
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
                        ((firstPlayerPoints + secondPlayerPoints) % 2 == 1 && lastPoint?.currentServe == firstPlayerId) -> secondPlayerId
                        ((firstPlayerPoints + secondPlayerPoints) % 2 == 1 && lastPoint?.currentServe == secondPlayerId) -> firstPlayerId
                        else -> lastPoint?.currentServe ?: matchEntity.firstPlayerServe!!
                    }
                } else {
                    isFirstPlayerWonGame = when {
                        changeScoreBody.scoreType == ScoreType.GAME && changeScoreBody.playerId == firstPlayerId.toString() -> true
                        currentSetTemplate.decidingPoint -> firstPlayerPoints == 4
                        else -> (firstPlayerPoints == 4 && secondPlayerPoints < 3) || (firstPlayerPoints > 4 && firstPlayerPoints - secondPlayerPoints == 2)
                    }

                    isSecondPlayerWonGame = when {
                        changeScoreBody.scoreType == ScoreType.GAME && changeScoreBody.playerId == secondPlayerId.toString() -> true
                        currentSetTemplate.decidingPoint -> secondPlayerPoints == 4
                        else -> (secondPlayerPoints == 4 && firstPlayerPoints < 3) || (secondPlayerPoints > 4 && secondPlayerPoints - firstPlayerPoints == 2)
                    }
                }


                if (isFirstPlayerWonGame || isSecondPlayerWonGame) {
                    scoreType = ScoreType.GAME
                    currentServe =
                        if (currentServe == firstPlayerId) secondPlayerId else firstPlayerId


                    firstPlayerPoints = when {
                        // для итогового результата супер-тайбрейка берем количество очков в гейме
                        currentSetMode == SpecialSetMode.SUPER_TIEBREAK -> firstPlayerPoints
                        else -> currentSetFirstPlayerPoints
                    }
                    secondPlayerPoints = when {
                        // для итогового результата супер-тайбрейка берем количество очков в гейме
                        currentSetMode == SpecialSetMode.SUPER_TIEBREAK -> secondPlayerPoints
                        else -> currentSetSecondPlayerPoints
                    }

                    if (isFirstPlayerWonGame) {
                        firstPlayerPoints++
                    } else {
                        secondPlayerPoints++
                    }
                }

                val gamesToWin = currentSetTemplate.gamesToWin

                val isFirstPlayerWonSet = when {
                    isTiebreakMode -> isFirstPlayerWonGame
                    else -> (firstPlayerPoints == gamesToWin && secondPlayerPoints < gamesToWin - 1) || (firstPlayerPoints > gamesToWin && firstPlayerPoints - secondPlayerPoints == 2)
                }

                val isSecondPlayerWonSet = when {
                    isTiebreakMode-> isSecondPlayerWonGame
                    else -> (secondPlayerPoints == gamesToWin && firstPlayerPoints < gamesToWin - 1) || (secondPlayerPoints > gamesToWin && secondPlayerPoints - firstPlayerPoints == 2)
                }

                if (isFirstPlayerWonSet || isSecondPlayerWonSet) {
                    scoreType = ScoreType.SET
                    if (isTiebreakMode) {
                        currentServe = when (currentSet?.currentServe) {
                            firstPlayerId -> secondPlayerId
                            secondPlayerId -> firstPlayerId
                            else -> {
                                if (setNumber % 2 == 0) {
                                    if (matchEntity.firstPlayerServe!! == firstPlayerId) {
                                        secondPlayerId
                                    } else {
                                        firstPlayerId
                                    }
                                } else {
                                    matchEntity.firstPlayerServe!!
                                }
                            }
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

                val newLastPointNumber = lastPointNumber + 1

                if (scoreType == ScoreType.SET) {
                    val previousSets = matchLogRepository.getPreviousSets(matchId, newLastPointNumber)

                    val (firstPlayerSetsWon, secondPlayerSetsWon) =
                        previousSets.partition { it.firstPlayerPoints > it.secondPlayerPoints }
                            .let { it.first.size to it.second.size }

                    if (firstPlayerSetsWon == setsToWin || secondPlayerSetsWon == setsToWin) {
                        val winnerPlayerId = if (firstPlayerSetsWon == setsToWin) firstPlayerId else secondPlayerId
                        matchRepository.updateWinner(matchId = matchId, winnerPlayerId = winnerPlayerId)
                    }
                }

                val matchDto = buildMatchById(matchId, newLastPointNumber)

                MatchObserver.notifyChange(matchDto)
            } else throw BadRequestException("Incorrect id")
        }
    }

    private fun findSetTemplate(matchEntity: MatchEntity, setNumber: Int, setsToWin: Int): SetTemplateEntity {
        val isDecidingSet = setNumber == 2 * setsToWin - 1

        val currentSetTemplate = if (isDecidingSet) {
            setTemplateRepository.getSetTemplateById(matchEntity.decidingSet)
        } else {
            setTemplateRepository.getSetTemplateById(matchEntity.regularSet)
        } ?: throw NotFoundException("No set template found!")

        return currentSetTemplate
    }

    private fun calculateCurrentSetMode(currentSetTemplate: SetTemplateEntity): SpecialSetMode? = when {
        currentSetTemplate.gamesToWin == 1 && currentSetTemplate.tiebreakMode == TiebreakMode.EARLY -> SpecialSetMode.SUPER_TIEBREAK
        currentSetTemplate.gamesToWin > 10 -> SpecialSetMode.ENDLESS
        else -> null
    }

    suspend fun undoPoint(matchId: Int) {
        return dbQuery {
            if (matchId != 0) {

                val matchEntity =
                    matchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

                val lastPointInTable = matchLogRepository.getLastPoint(matchId)

                val pointShift = matchEntity.pointShift

                val newPointShift = pointShift - 1

                val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + newPointShift

                if (lastPointNumber < 0) throw BadRequestException("Cannot undo the point")

                matchEntity.winner?.let {
                    matchRepository.updateWinner(matchId = matchId, winnerPlayerId = null)
                }

                matchRepository.updatePointShift(matchId = matchId, newPointShift = newPointShift)

                val matchDto = buildMatchById(matchId, lastPointNumber)

                MatchObserver.notifyChange(matchDto)
            } else throw BadRequestException("Incorrect id")
        }
    }

    suspend fun redoPoint(matchId: Int) {
        return dbQuery {
            if (matchId != 0) {

                val matchEntity =
                    matchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

                val lastPointInTable = matchLogRepository.getLastPoint(matchId)

                val pointShift = matchEntity.pointShift

                if (pointShift == 0) throw BadRequestException("Cannot redo the point")

                val newPointShift = pointShift + 1

                val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + newPointShift

                matchRepository.updatePointShift(matchId = matchId, newPointShift = newPointShift)

                val lastPoint = matchLogRepository.getLastPoint(matchId, lastPointNumber)

                if (lastPoint?.scoreType == ScoreType.SET) {
                    val previousSets = matchLogRepository.getPreviousSets(matchId, lastPointNumber)

                    val setsToWin = matchEntity.setsToWin
                    val firstPlayerId = matchEntity.firstPlayer.id
                    val secondPlayerId = matchEntity.secondPlayer.id

                    val (firstPlayerSetsWon, secondPlayerSetsWon) =
                        previousSets.partition { it.firstPlayerPoints > it.secondPlayerPoints }
                            .let { it.first.size to it.second.size }

                    if (firstPlayerSetsWon == setsToWin || secondPlayerSetsWon == setsToWin) {
                        val winnerPlayerId = if (firstPlayerSetsWon == setsToWin) firstPlayerId else secondPlayerId
                        matchRepository.updateWinner(matchId = matchId, winnerPlayerId = winnerPlayerId)
                    }
                }

                val matchDto = buildMatchById(matchId, lastPointNumber)

                MatchObserver.notifyChange(matchDto)
            } else throw BadRequestException("Incorrect id")
        }
    }
}