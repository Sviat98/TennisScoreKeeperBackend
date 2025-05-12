package com.bashkevich.tennisscorekeeperbackend.feature.match.doubles

import com.bashkevich.tennisscorekeeperbackend.feature.match.websocket.MatchObserver
import com.bashkevich.tennisscorekeeperbackend.feature.match_log.DoublesMatchLogRepository
import com.bashkevich.tennisscorekeeperbackend.feature.participant.doubles.DoublesParticipantRepository
import com.bashkevich.tennisscorekeeperbackend.feature.set_template.SetTemplateRepository
import com.bashkevich.tennisscorekeeperbackend.model.match.ChangeScoreBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.ScoreType
import com.bashkevich.tennisscorekeeperbackend.model.match.ServeBody
import com.bashkevich.tennisscorekeeperbackend.model.match.ShortMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.SpecialSetMode
import com.bashkevich.tennisscorekeeperbackend.model.match.TennisGameDto
import com.bashkevich.tennisscorekeeperbackend.model.match.TennisSetDto
import com.bashkevich.tennisscorekeeperbackend.model.match.doubles.DoublesMatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.match.toShortMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.toTennisGameDto
import com.bashkevich.tennisscorekeeperbackend.model.match.toTennisSetDto
import com.bashkevich.tennisscorekeeperbackend.model.match_log.doubles.DoublesMatchLogEvent
import com.bashkevich.tennisscorekeeperbackend.model.participant.toDto
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateEntity
import com.bashkevich.tennisscorekeeperbackend.model.set_template.TiebreakMode
import com.bashkevich.tennisscorekeeperbackend.plugins.validateBody
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException

class DoublesMatchService(
    private val doublesMatchRepository: DoublesMatchRepository,
    private val doublesMatchLogRepository: DoublesMatchLogRepository,
    private val setTemplateRepository: SetTemplateRepository,
    private val doublesParticipantRepository: DoublesParticipantRepository,
) {
    suspend fun addMatch(tournamentId: Int, matchBody: MatchBody): ShortMatchDto {
        validateBody(matchBody) {
            val firstParticipantId = matchBody.firstParticipant.id.toInt()
            val secondParticipantId = matchBody.secondParticipant.id.toInt()

            val regularSetId = matchBody.regularSet.toInt()
            val decidingSetId = matchBody.decidingSet.toInt()

            val firstParticipant = doublesParticipantRepository.getParticipantById(firstParticipantId)
            val secondParticipant = doublesParticipantRepository.getParticipantById(secondParticipantId)

            val regularSet = setTemplateRepository.getSetTemplateById(regularSetId)
            val decidingSet = setTemplateRepository.getSetTemplateById(decidingSetId)

            when {
                firstParticipant == null -> "First player does not exist"
                secondParticipant == null -> "Second player does not exist"
                regularSet == null -> "Regular set does not exist"
                decidingSet == null -> "Deciding set does not exist"
                else -> ""
            }
        }

        val newMatchId = doublesMatchRepository.addMatch(tournamentId, matchBody).value

        val shortMatchDto = doublesMatchRepository.getMatchById(newMatchId)

        return shortMatchDto!!.toShortMatchDto()
    }

    fun getMatches(tournamentId: Int): List<ShortMatchDto> {
        return doublesMatchRepository.getMatches(tournamentId).map { matchEntity ->
            matchEntity.toShortMatchDto()
        }
    }

    fun getMatchById(matchId: Int): MatchDto {
        if (matchId == 0) throw BadRequestException("Incorrect id")
        val matchEntity = doublesMatchRepository.getMatchById(matchId)
            ?: throw NotFoundException("No match found!")

        val lastPointInTable = doublesMatchLogRepository.getLastPoint(matchId)

        val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + matchEntity.pointShift

        val matchDto = buildMatchById(matchId, lastPointNumber)

        return matchDto
    }

    suspend fun updateServe(matchId: Int, serveBody: ServeBody) {
        if (matchId == 0) throw BadRequestException("Incorrect id")
        val firstServeParticipantId = serveBody.servingParticipantId.toInt()

        val matchEntity = doublesMatchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

        validateBody(serveBody) {
            if (firstServeParticipantId !in listOf(
                    matchEntity.firstParticipant.id.value,
                    matchEntity.secondParticipant.id.value
                )
            )
                "Serve player id is not in match" else ""
        }

        doublesMatchRepository.updateServe(matchId, firstServeParticipantId)

        val matchDto = buildMatchById(matchId, 0)

        MatchObserver.notifyChange(matchDto)
    }

    private fun buildMatchById(matchId: Int, lastPointNumber: Int): MatchDto {
        val matchEntity = doublesMatchRepository.getMatchById(matchId)
            ?: throw NotFoundException("No match found!")

        // для предыдущих партий всгде возвращаем ORDINARY,здесь нам нужен только сам счет
        val previousSets =
            doublesMatchLogRepository.getPreviousSets(matchId, lastPointNumber).map { it.toTennisSetDto() }

        val setNumber = previousSets.size + 1

        val lastPoint = doublesMatchLogRepository.getLastPoint(matchId, lastPointNumber)

        val setsToWin = matchEntity.setsToWin

        val currentSetTemplate = findSetTemplate(matchEntity, setNumber, setsToWin)

        val currentSetMode = calculateCurrentSetMode(currentSetTemplate)

        val firstParticipantId = matchEntity.firstParticipant.id.value
        val secondParticipantId = matchEntity.secondParticipant.id.value


        val currentServe = lastPoint?.currentServe ?: matchEntity.firstServe?.id?.value
        val winnerParticipantId = matchEntity.winner?.id?.value
        val firstParticipant = matchEntity.firstParticipant.toDto(
            displayName = matchEntity.firstParticipantDisplayName,
            servingParticipantId = currentServe,
            servingInPairPlayerId = null,
            winningParticipantId = winnerParticipantId
        )
        //?: throw NotFoundException("Player with id = $firstParticipantId not found")

        val secondParticipant = matchEntity.firstParticipant.toDto(
            displayName = matchEntity.firstParticipantDisplayName,
            servingParticipantId = currentServe,
            servingInPairPlayerId = null,
            winningParticipantId = winnerParticipantId
        )
        //?: throw NotFoundException("Player with id = $secondParticipantId not found")

        val lastGame = doublesMatchLogRepository.getCurrentSet(matchId, setNumber, lastPointNumber)

        val currentSet = when {
            lastPoint?.scoreType == ScoreType.SET -> TennisSetDto(0, 0, currentSetMode)
            currentSetMode == SpecialSetMode.SUPER_TIEBREAK -> TennisSetDto(
                firstPlayerGames = lastPoint?.firstParticipantPoints ?: 0,
                lastPoint?.secondParticipantPoints ?: 0,
                currentSetMode
            )

            else -> lastGame?.toTennisSetDto(
                specialSetMode = currentSetMode
            )
                ?: TennisSetDto(firstPlayerGames = 0, secondPlayerGames = 0, specialSetMode = currentSetMode)
        }

        val currentGame = when {
            currentSetMode == SpecialSetMode.SUPER_TIEBREAK -> TennisGameDto("0", "0")
            lastPoint?.scoreType in listOf(ScoreType.GAME, ScoreType.SET) -> TennisGameDto("0", "0")
            else -> lastPoint?.toTennisGameDto() ?: TennisGameDto("0", "0")
        }

        val matchDto = MatchDto(
            id = matchId.toString(),
            pointShift = matchEntity.pointShift,
            firstPlayer = firstParticipant,
            secondPlayer = secondParticipant,
            previousSets = previousSets,
            currentSet = currentSet,
            currentGame = currentGame
        )

        return matchDto
    }

    suspend fun updateScore(matchId: Int, changeScoreBody: ChangeScoreBody) {
        if (matchId == 0) throw BadRequestException("Incorrect id")
        val scoringPlayerId = changeScoreBody.participantId.toInt()

        val matchEntity = doublesMatchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

        val firstParticipantId = matchEntity.firstParticipant.id.value

        val secondParticipantId = matchEntity.secondParticipant.id.value


        validateBody(changeScoreBody) {
            if (scoringPlayerId !in listOf(firstParticipantId, secondParticipantId))
                "Scoring player id is not in match" else ""
        }

        val lastPointInTable = doublesMatchLogRepository.getLastPoint(matchId)

        val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + matchEntity.pointShift

        val lastPoint = doublesMatchLogRepository.getLastPoint(matchId, lastPointNumber)

        if (matchEntity.pointShift < 0) {
            doublesMatchRepository.updatePointShift(matchId = matchId, newPointShift = 0)

            doublesMatchLogRepository.removeEvents(matchId = matchId, pointNumber = lastPointNumber)
        }

        val setsToWin = matchEntity.setsToWin

        var setNumber = lastPoint?.setNumber ?: 1

        var pointNumber = lastPoint?.pointNumber ?: 0

        pointNumber++

        val firstParticipantToServeInMatch = matchEntity.firstServe!!.id.value

        var currentServe = lastPoint?.currentServe ?: firstParticipantToServeInMatch

        var firstParticipantPoints = 0
        var secondParticipantPoints = 0
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
            firstParticipantPoints = lastPoint?.firstParticipantPoints ?: 0
            secondParticipantPoints = lastPoint?.secondParticipantPoints ?: 0
        }

        when {
            firstParticipantId == scoringPlayerId -> firstParticipantPoints++
            secondParticipantId == scoringPlayerId -> secondParticipantPoints++
        }

        val currentSetTemplate = findSetTemplate(matchEntity, setNumber, setsToWin)

        val currentSetMode = calculateCurrentSetMode(currentSetTemplate)

        val currentSet = doublesMatchLogRepository.getCurrentSet(matchId, setNumber, lastPointNumber)

        val currentSetFirstPlayerPoints = currentSet?.firstParticipantPoints ?: 0
        val currentSetSecondPlayerPoints = currentSet?.secondParticipantPoints ?: 0

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
                if (secondParticipantPoints < tiebreakPointsToWin - 1) firstParticipantPoints == tiebreakPointsToWin else
                    firstParticipantPoints - secondParticipantPoints == 2

            isSecondPlayerWonGame =
                if (firstParticipantPoints < tiebreakPointsToWin - 1) secondParticipantPoints == tiebreakPointsToWin
                else secondParticipantPoints - firstParticipantPoints == 2
            scoreType = ScoreType.TIEBREAK_POINT

            currentServe = when {
                ((firstParticipantPoints + secondParticipantPoints) % 2 == 1 && lastPoint?.currentServe == firstParticipantId) -> secondParticipantId
                ((firstParticipantPoints + secondParticipantPoints) % 2 == 1 && lastPoint?.currentServe == secondParticipantId) -> firstParticipantId
                else -> lastPoint?.currentServe ?: firstParticipantToServeInMatch
            }
        } else {
            isFirstPlayerWonGame = when {
                changeScoreBody.scoreType == ScoreType.GAME -> scoringPlayerId == firstParticipantId
                currentSetTemplate.decidingPoint -> firstParticipantPoints == 4
                else -> (firstParticipantPoints == 4 && secondParticipantPoints < 3) || (firstParticipantPoints > 4 && firstParticipantPoints - secondParticipantPoints == 2)
            }

            isSecondPlayerWonGame = when {
                changeScoreBody.scoreType == ScoreType.GAME -> scoringPlayerId == secondParticipantId
                currentSetTemplate.decidingPoint -> secondParticipantPoints == 4
                else -> (secondParticipantPoints == 4 && firstParticipantPoints < 3) || (secondParticipantPoints > 4 && secondParticipantPoints - firstParticipantPoints == 2)
            }
        }


        if ((isFirstPlayerWonGame || isSecondPlayerWonGame) && currentSetMode != SpecialSetMode.SUPER_TIEBREAK) {
            // для супер-тайбрейка обработка смены подачи ниже, а количество геймов мы не увеличиваем
            scoreType = ScoreType.GAME
            currentServe =
                if (currentServe == firstParticipantId) secondParticipantId else firstParticipantId


            firstParticipantPoints = currentSetFirstPlayerPoints
            secondParticipantPoints = currentSetSecondPlayerPoints

            if (isFirstPlayerWonGame) {
                firstParticipantPoints++
            } else {
                secondParticipantPoints++
            }
        }

        val gamesToWin = currentSetTemplate.gamesToWin

        val isFirstPlayerWonSet = when {
            isTiebreakMode -> isFirstPlayerWonGame
            else -> (firstParticipantPoints == gamesToWin && secondParticipantPoints < gamesToWin - 1) || (firstParticipantPoints > gamesToWin && firstParticipantPoints - secondParticipantPoints == 2)
        }

        val isSecondPlayerWonSet = when {
            isTiebreakMode -> isSecondPlayerWonGame
            else -> (secondParticipantPoints == gamesToWin && firstParticipantPoints < gamesToWin - 1) || (secondParticipantPoints > gamesToWin && secondParticipantPoints - firstParticipantPoints == 2)
        }

        if (isFirstPlayerWonSet || isSecondPlayerWonSet) {
            scoreType = ScoreType.SET
            if (isTiebreakMode) {
                currentServe = when (currentSet?.currentServe) {
                    firstParticipantId -> secondParticipantId
                    secondParticipantId -> firstParticipantId
                    else -> {
                        if (setNumber % 2 == 0) {
                            if (firstParticipantToServeInMatch == firstParticipantId) {
                                secondParticipantId
                            } else {
                                firstParticipantId
                            }
                        } else {
                            firstParticipantToServeInMatch
                        }
                    }
                }
            }
        }

        val doublesMatchLogEvent = DoublesMatchLogEvent(
            matchId = matchId,
            setNumber = setNumber,
            pointNumber = pointNumber,
            scoreType = scoreType,
            currentServe = currentServe,
            currentServeInPair = 0,
            firstParticipantPoints = firstParticipantPoints,
            secondParticipantPoints = secondParticipantPoints,
        )

        doublesMatchLogRepository.insertMatchLogEvent(doublesMatchLogEvent)

        val newLastPointNumber = lastPointNumber + 1

        if (scoreType == ScoreType.SET) {
            val previousSets = doublesMatchLogRepository.getPreviousSets(matchId, newLastPointNumber)

            val (firstParticipantSetsWon, secondParticipantSetsWon) =
                previousSets.partition { it.firstParticipantPoints > it.secondParticipantPoints }
                    .let { it.first.size to it.second.size }

            if (firstParticipantSetsWon == setsToWin || secondParticipantSetsWon == setsToWin) {
                val winnerParticipantId = if (firstParticipantSetsWon == setsToWin) firstParticipantId else secondParticipantId
                doublesMatchRepository.updateWinner(matchId = matchId, winnerParticipantId = winnerParticipantId)
            }
        }

        val matchDto = buildMatchById(matchId, newLastPointNumber)

        MatchObserver.notifyChange(matchDto)
    }

    private fun findSetTemplate(matchEntity: DoublesMatchEntity, setNumber: Int, setsToWin: Int): SetTemplateEntity {
        val isDecidingSet = setNumber == 2 * setsToWin - 1

        val currentSetTemplate = if (isDecidingSet) {
            setTemplateRepository.getSetTemplateById(matchEntity.decidingSet.id.value)
                ?: throw NotFoundException("No deciding set template found in match!")
        } else {
            setTemplateRepository.getSetTemplateById(matchEntity.regularSet.id.value)
                ?: throw NotFoundException("No regular set template found in match!")
        }

        return currentSetTemplate
    }

    private fun calculateCurrentSetMode(currentSetTemplate: SetTemplateEntity): SpecialSetMode? = when {
        currentSetTemplate.gamesToWin == 1 && currentSetTemplate.tiebreakMode == TiebreakMode.EARLY -> SpecialSetMode.SUPER_TIEBREAK
        currentSetTemplate.gamesToWin > 10 -> SpecialSetMode.ENDLESS
        else -> null
    }

    suspend fun undoPoint(matchId: Int) {
        if (matchId == 0) throw BadRequestException("Incorrect id")
        val matchEntity =
            doublesMatchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

        val lastPointInTable = doublesMatchLogRepository.getLastPoint(matchId)

        val pointShift = matchEntity.pointShift

        val newPointShift = pointShift - 1

        val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + newPointShift

        if (lastPointNumber < 0) throw BadRequestException("Cannot undo the point")

        matchEntity.winner?.let {
            doublesMatchRepository.updateWinner(matchId = matchId, winnerParticipantId = null)
        }

        doublesMatchRepository.updatePointShift(matchId = matchId, newPointShift = newPointShift)

        val matchDto = buildMatchById(matchId, lastPointNumber)

        MatchObserver.notifyChange(matchDto)
    }

    suspend fun redoPoint(matchId: Int) {
        if (matchId == 0) throw BadRequestException("Incorrect id")

        val matchEntity =
            doublesMatchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

        val lastPointInTable = doublesMatchLogRepository.getLastPoint(matchId)

        val pointShift = matchEntity.pointShift

        if (pointShift == 0) throw BadRequestException("Cannot redo the point")

        val newPointShift = pointShift + 1

        val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + newPointShift

        doublesMatchRepository.updatePointShift(matchId = matchId, newPointShift = newPointShift)

        val lastPoint = doublesMatchLogRepository.getLastPoint(matchId, lastPointNumber)

        if (lastPoint?.scoreType == ScoreType.SET) {
            val previousSets = doublesMatchLogRepository.getPreviousSets(matchId, lastPointNumber)

            val setsToWin = matchEntity.setsToWin
            val firstParticipantId = matchEntity.firstParticipant.id.value
            val secondParticipantId = matchEntity.secondParticipant.id.value

            val (firstParticipantSetsWon, secondParticipantSetsWon) =
                previousSets.partition { it.firstParticipantPoints > it.secondParticipantPoints }
                    .let { it.first.size to it.second.size }

            if (firstParticipantSetsWon == setsToWin || secondParticipantSetsWon == setsToWin) {
                val winnerPlayerId =
                    if (firstParticipantSetsWon == setsToWin) firstParticipantId else secondParticipantId
                doublesMatchRepository.updateWinner(matchId = matchId, winnerParticipantId = winnerPlayerId)
            }
        }

        val matchDto = buildMatchById(matchId, lastPointNumber)

        MatchObserver.notifyChange(matchDto)
    }
}