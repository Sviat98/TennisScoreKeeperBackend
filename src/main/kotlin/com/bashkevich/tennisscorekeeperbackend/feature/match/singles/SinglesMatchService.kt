package com.bashkevich.tennisscorekeeperbackend.feature.match.singles

import com.bashkevich.tennisscorekeeperbackend.feature.match.websocket.MatchObserver
import com.bashkevich.tennisscorekeeperbackend.feature.match_log.SinglesMatchLogRepository
import com.bashkevich.tennisscorekeeperbackend.feature.participant.singles.SinglesParticipantRepository
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
import com.bashkevich.tennisscorekeeperbackend.model.match.singles.SinglesMatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.match.toShortMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.toTennisGameDto
import com.bashkevich.tennisscorekeeperbackend.model.match.toTennisSetDto
import com.bashkevich.tennisscorekeeperbackend.model.match_log.singles.SinglesMatchLogEvent
import com.bashkevich.tennisscorekeeperbackend.model.participant.toDto
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateEntity
import com.bashkevich.tennisscorekeeperbackend.model.set_template.TiebreakMode
import com.bashkevich.tennisscorekeeperbackend.plugins.validateBody
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException

class SinglesMatchService(
    private val singlesMatchRepository: SinglesMatchRepository,
    private val singlesMatchLogRepository: SinglesMatchLogRepository,
    private val setTemplateRepository: SetTemplateRepository,
    private val singlesParticipantRepository: SinglesParticipantRepository,
) {
    suspend fun addMatch(tournamentId: Int, matchBody: MatchBody): ShortMatchDto {

        validateBody(matchBody) {
            val firstParticipantId = matchBody.firstParticipant.id.toInt()
            val secondParticipantId = matchBody.secondParticipant.id.toInt()

            val regularSetId = matchBody.regularSet.toInt()
            val decidingSetId = matchBody.decidingSet.toInt()

            val firstParticipant = singlesParticipantRepository.getParticipantById(firstParticipantId)
            val secondParticipant = singlesParticipantRepository.getParticipantById(secondParticipantId)

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

        val newMatchId = singlesMatchRepository.addMatch(tournamentId, matchBody).value

        val shortMatchDto = singlesMatchRepository.getMatchById(newMatchId)

        return shortMatchDto!!.toShortMatchDto()
    }

    fun getMatches(tournamentId: Int): List<ShortMatchDto> {
        return singlesMatchRepository.getMatches(tournamentId).map { matchEntity ->
            matchEntity.toShortMatchDto()
        }
    }

    fun getMatchById(matchId: Int): MatchDto {
        if (matchId == 0) throw BadRequestException("Incorrect id")
        val matchEntity = singlesMatchRepository.getMatchById(matchId)
            ?: throw NotFoundException("No match found!")

        val lastPointInTable = singlesMatchLogRepository.getLastPoint(matchId)

        val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + matchEntity.pointShift

        val matchDto = buildMatchById(matchId, lastPointNumber)

        return matchDto
    }

    suspend fun updateServe(matchId: Int, serveBody: ServeBody) {
        if (matchId == 0) throw BadRequestException("Incorrect id")
        val firstServeParticipantId = serveBody.servingParticipantId.toInt()

        val matchEntity = singlesMatchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

        validateBody(serveBody) {
            if (firstServeParticipantId !in listOf(
                    matchEntity.firstParticipant.id.value,
                    matchEntity.secondParticipant.id.value
                )
            )
                "Serve player id is not in match" else ""
        }

        singlesMatchRepository.updateServe(matchId, firstServeParticipantId)

        val matchDto = buildMatchById(matchId, 0)

        MatchObserver.notifyChange(matchDto)
    }

    private fun buildMatchById(matchId: Int, lastPointNumber: Int): MatchDto {
        val matchEntity = singlesMatchRepository.getMatchById(matchId)
            ?: throw NotFoundException("No match found!")

        // для предыдущих партий всгде возвращаем ORDINARY,здесь нам нужен только сам счет
        val previousSets =
            singlesMatchLogRepository.getPreviousSets(matchId, lastPointNumber).map { it.toTennisSetDto() }

        val setNumber = previousSets.size + 1

        val lastPoint = singlesMatchLogRepository.getLastPoint(matchId, lastPointNumber)

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
                winningParticipantId = winnerParticipantId
            )
            //?: throw NotFoundException("Player with id = $firstParticipantId not found")

        val secondParticipant = matchEntity.secondParticipant.toDto(
                displayName = matchEntity.secondParticipantDisplayName,
                servingParticipantId = currentServe,
                winningParticipantId = winnerParticipantId
            )
            //?: throw NotFoundException("Player with id = $secondParticipantId not found")

        val lastGame = singlesMatchLogRepository.getCurrentSet(matchId, setNumber, lastPointNumber)

        val currentSet = when {
            lastPoint?.scoreType == ScoreType.SET -> TennisSetDto(0, 0, currentSetMode)
            currentSetMode == SpecialSetMode.SUPER_TIEBREAK -> TennisSetDto(
                firstParticipantGames = lastPoint?.firstParticipantPoints ?: 0,
                secondParticipantGames = lastPoint?.secondParticipantPoints ?: 0,
                specialSetMode = currentSetMode
            )

            else -> lastGame?.toTennisSetDto(
                specialSetMode = currentSetMode
            )
                ?: TennisSetDto(firstParticipantGames = 0, secondParticipantGames = 0, specialSetMode = currentSetMode)
        }

        val currentGame = when {
            currentSetMode == SpecialSetMode.SUPER_TIEBREAK -> TennisGameDto("0", "0")
            lastPoint?.scoreType in listOf(ScoreType.GAME, ScoreType.SET) -> TennisGameDto("0", "0")
            else -> lastPoint?.toTennisGameDto() ?: TennisGameDto("0", "0")
        }

        val matchDto = MatchDto(
            id = matchId.toString(),
            pointShift = matchEntity.pointShift,
            firstParticipant = firstParticipant,
            secondParticipant = secondParticipant,
            previousSets = previousSets,
            currentSet = currentSet,
            currentGame = currentGame
        )

        return matchDto
    }

    suspend fun updateScore(matchId: Int, changeScoreBody: ChangeScoreBody) {
        if (matchId == 0) throw BadRequestException("Incorrect id")
        val scoringParticipantId = changeScoreBody.participantId.toInt()

        val matchEntity = singlesMatchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

        val firstParticipantId = matchEntity.firstParticipant.id.value

        val secondParticipantId = matchEntity.secondParticipant.id.value

        validateBody(changeScoreBody) {
            if (scoringParticipantId !in listOf(firstParticipantId, secondParticipantId))
                "Scoring player id is not in match" else ""
        }

        val firstParticipantToServeInMatch = matchEntity.firstServe!!.id.value

        val secondParticipantToServeInMatch =
            if (firstParticipantToServeInMatch == firstParticipantId) secondParticipantId else firstParticipantId

        val participantServingOrder = listOf(firstParticipantToServeInMatch, secondParticipantToServeInMatch)

        val lastPointInTable = singlesMatchLogRepository.getLastPoint(matchId)

        val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + matchEntity.pointShift

        val lastPoint = singlesMatchLogRepository.getLastPoint(matchId, lastPointNumber)

        if (matchEntity.pointShift < 0) {
            singlesMatchRepository.updatePointShift(matchId = matchId, newPointShift = 0)

            singlesMatchLogRepository.removeEvents(matchId = matchId, pointNumber = lastPointNumber)
        }

        val setsToWin = matchEntity.setsToWin


        var setNumber = lastPoint?.setNumber ?: 1

        var pointNumber = lastPoint?.pointNumber ?: 0

        pointNumber++

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
            firstParticipantId == scoringParticipantId -> firstParticipantPoints++
            secondParticipantId == scoringParticipantId -> secondParticipantPoints++
        }

        val currentSetTemplate = findSetTemplate(matchEntity, setNumber, setsToWin)

        val currentSetMode = calculateCurrentSetMode(currentSetTemplate)

        val currentSet = singlesMatchLogRepository.getCurrentSet(matchId, setNumber, lastPointNumber)

        val currentSetFirstParticipantPoints = currentSet?.firstParticipantPoints ?: 0
        val currentSetSecondParticipantPoints = currentSet?.secondParticipantPoints ?: 0

        var isFirstParticipantWonGame: Boolean
        var isSecondParticipantWonGame: Boolean

        val isTiebreakMode = when (currentSetTemplate.tiebreakMode) {
            // пример для сета до 6 геймов
            // EARLY - когда тайбрейк играется при сете 5-5
            //LATE - когда тайбрейк играется при сете 6-6
            TiebreakMode.EARLY -> currentSetFirstParticipantPoints == currentSetSecondParticipantPoints
                    && currentSetFirstParticipantPoints == currentSetTemplate.gamesToWin - 1

            TiebreakMode.LATE -> currentSetFirstParticipantPoints == currentSetSecondParticipantPoints && currentSetFirstParticipantPoints == currentSetTemplate.gamesToWin
            else -> false
        }

        if (isTiebreakMode) {
            val tiebreakPointsToWin = currentSetTemplate.tiebreakPointsToWin

            isFirstParticipantWonGame =
                if (secondParticipantPoints < tiebreakPointsToWin - 1) firstParticipantPoints == tiebreakPointsToWin else
                    firstParticipantPoints - secondParticipantPoints == 2

            isSecondParticipantWonGame =
                if (firstParticipantPoints < tiebreakPointsToWin - 1) secondParticipantPoints == tiebreakPointsToWin
                else secondParticipantPoints - firstParticipantPoints == 2
            scoreType = ScoreType.TIEBREAK_POINT

            currentServe = when {
                (firstParticipantPoints + secondParticipantPoints) % 2 == 1 -> calculateNextServe(participantServingOrder,currentServe)
                else -> currentServe
            }
        } else {
            isFirstParticipantWonGame = when {
                changeScoreBody.scoreType == ScoreType.GAME -> changeScoreBody.participantId == firstParticipantId.toString()
                currentSetTemplate.decidingPoint -> firstParticipantPoints == 4
                else -> (firstParticipantPoints == 4 && secondParticipantPoints < 3) || (firstParticipantPoints > 4 && firstParticipantPoints - secondParticipantPoints == 2)
            }

            isSecondParticipantWonGame = when {
                changeScoreBody.scoreType == ScoreType.GAME -> changeScoreBody.participantId == secondParticipantId.toString()
                currentSetTemplate.decidingPoint -> secondParticipantPoints == 4
                else -> (secondParticipantPoints == 4 && firstParticipantPoints < 3) || (secondParticipantPoints > 4 && secondParticipantPoints - firstParticipantPoints == 2)
            }
        }


        if ((isFirstParticipantWonGame || isSecondParticipantWonGame) && currentSetMode != SpecialSetMode.SUPER_TIEBREAK) {
            // для супер-тайбрейка обработка смены подачи ниже, а количество геймов мы не увеличиваем
            scoreType = ScoreType.GAME
            currentServe = calculateNextServe(participantServingOrder,currentServe)


            firstParticipantPoints = currentSetFirstParticipantPoints
            secondParticipantPoints = currentSetSecondParticipantPoints

            if (isFirstParticipantWonGame) {
                firstParticipantPoints++
            } else {
                secondParticipantPoints++
            }
        }

        val gamesToWin = currentSetTemplate.gamesToWin

        val isFirstParticipantWonSet = when {
            isTiebreakMode -> isFirstParticipantWonGame
            else -> (firstParticipantPoints == gamesToWin && secondParticipantPoints < gamesToWin - 1) || (firstParticipantPoints > gamesToWin && firstParticipantPoints - secondParticipantPoints == 2)
        }

        val isSecondParticipantWonSet = when {
            isTiebreakMode -> isSecondParticipantWonGame
            else -> (secondParticipantPoints == gamesToWin && firstParticipantPoints < gamesToWin - 1) || (secondParticipantPoints > gamesToWin && secondParticipantPoints - firstParticipantPoints == 2)
        }

        if (isFirstParticipantWonSet || isSecondParticipantWonSet) {
            scoreType = ScoreType.SET
            if (isTiebreakMode) {
                currentServe = when {
                    currentSet != null -> calculateNextServe(participantServingOrder, currentSet.currentServe)
                    setNumber % 2 == 0 -> secondParticipantToServeInMatch
                    else -> firstParticipantToServeInMatch
                }
            }
        }

        val singlesMatchLogEvent = SinglesMatchLogEvent(
            matchId = matchId,
            setNumber = setNumber,
            pointNumber = pointNumber,
            scoreType = scoreType,
            currentServe = currentServe,
            firstParticipantPoints = firstParticipantPoints,
            secondParticipantPoints = secondParticipantPoints
        )

        singlesMatchLogRepository.insertMatchLogEvent(singlesMatchLogEvent)

        val newLastPointNumber = lastPointNumber + 1

        if (scoreType == ScoreType.SET) {
            val previousSets = singlesMatchLogRepository.getPreviousSets(matchId, newLastPointNumber)

            val (firstParticipantSetsWon, secondParticipantSetsWon) =
                previousSets.partition { it.firstParticipantPoints > it.secondParticipantPoints }
                    .let { it.first.size to it.second.size }

            if (firstParticipantSetsWon == setsToWin || secondParticipantSetsWon == setsToWin) {
                val winnerParticipantId = if (firstParticipantSetsWon == setsToWin) firstParticipantId else secondParticipantId
                singlesMatchRepository.updateWinner(matchId = matchId, winnerParticipantId = winnerParticipantId)
            }
        }

        val matchDto = buildMatchById(matchId, newLastPointNumber)

        MatchObserver.notifyChange(matchDto)
    }

    private fun findSetTemplate(matchEntity: SinglesMatchEntity, setNumber: Int, setsToWin: Int): SetTemplateEntity {
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

    private fun calculateNextServe(serveOrder: List<Int>, currentServe: Int): Int {
        val newServeIndex = serveOrder.indexOf(currentServe) + 1
        val size = serveOrder.size

        return serveOrder[newServeIndex % size]
    }

    suspend fun undoPoint(matchId: Int) {
        if (matchId == 0) throw BadRequestException("Incorrect id")
        val matchEntity =
            singlesMatchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

        val lastPointInTable = singlesMatchLogRepository.getLastPoint(matchId)

        val pointShift = matchEntity.pointShift

        val newPointShift = pointShift - 1

        val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + newPointShift

        if (lastPointNumber < 0) throw BadRequestException("Cannot undo the point")

        matchEntity.winner?.let {
            singlesMatchRepository.updateWinner(matchId = matchId, winnerParticipantId = null)
        }

        singlesMatchRepository.updatePointShift(matchId = matchId, newPointShift = newPointShift)

        val matchDto = buildMatchById(matchId, lastPointNumber)

        MatchObserver.notifyChange(matchDto)
    }

    suspend fun redoPoint(matchId: Int) {
        if (matchId == 0) throw BadRequestException("Incorrect id")

        val matchEntity =
            singlesMatchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

        val lastPointInTable = singlesMatchLogRepository.getLastPoint(matchId)

        val pointShift = matchEntity.pointShift

        if (pointShift == 0) throw BadRequestException("Cannot redo the point")

        val newPointShift = pointShift + 1

        val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + newPointShift

        singlesMatchRepository.updatePointShift(matchId = matchId, newPointShift = newPointShift)

        val lastPoint = singlesMatchLogRepository.getLastPoint(matchId, lastPointNumber)

        if (lastPoint?.scoreType == ScoreType.SET) {
            val previousSets = singlesMatchLogRepository.getPreviousSets(matchId, lastPointNumber)

            val setsToWin = matchEntity.setsToWin
            val firstParticipantId = matchEntity.firstParticipant.id.value
            val secondParticipantId = matchEntity.secondParticipant.id.value

            val (firstParticipantSetsWon, secondParticipantSetsWon) =
                previousSets.partition { it.firstParticipantPoints > it.secondParticipantPoints }
                    .let { it.first.size to it.second.size }

            if (firstParticipantSetsWon == setsToWin || secondParticipantSetsWon == setsToWin) {
                val winnerParticipantId =
                    if (firstParticipantSetsWon == setsToWin) firstParticipantId else secondParticipantId
                singlesMatchRepository.updateWinner(matchId = matchId, winnerParticipantId = winnerParticipantId)
            }
        }

        val matchDto = buildMatchById(matchId, lastPointNumber)

        MatchObserver.notifyChange(matchDto)
    }
}