package com.bashkevich.tennisscorekeeperbackend.feature.match.singles

import com.bashkevich.tennisscorekeeperbackend.feature.match.websocket.MatchObserver
import com.bashkevich.tennisscorekeeperbackend.feature.match_log.SinglesMatchLogRepository
import com.bashkevich.tennisscorekeeperbackend.feature.participant.singles.SinglesParticipantRepository
import com.bashkevich.tennisscorekeeperbackend.feature.set_template.SetTemplateRepository
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ChangeScoreBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchStatus
import com.bashkevich.tennisscorekeeperbackend.model.match.body.MatchStatusBody
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ScoreType
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ServeBody
import com.bashkevich.tennisscorekeeperbackend.model.match.ShortMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.SpecialSetMode
import com.bashkevich.tennisscorekeeperbackend.model.match.TennisSetDto
import com.bashkevich.tennisscorekeeperbackend.model.match.singles.SinglesMatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.match.toShortMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.toTennisGameDto
import com.bashkevich.tennisscorekeeperbackend.model.match.toTennisSetDto
import com.bashkevich.tennisscorekeeperbackend.model.match_log.singles.SinglesMatchLogEvent
import com.bashkevich.tennisscorekeeperbackend.model.participant.toParticipantInMatchDto
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

            val regularSetId = matchBody.regularSet?.toInt() ?: 0
            val decidingSetId = matchBody.decidingSet.toInt()

            val firstParticipant = singlesParticipantRepository.getParticipantById(firstParticipantId)
            val secondParticipant = singlesParticipantRepository.getParticipantById(secondParticipantId)

            val setsToWin = matchBody.setsToWin

            val regularSet = setTemplateRepository.getSetTemplateById(regularSetId)
            val decidingSet = setTemplateRepository.getSetTemplateById(decidingSetId)

            when {
                firstParticipant == null -> "First player does not exist"
                secondParticipant == null -> "Second player does not exist"
                setsToWin > 1 && regularSet == null -> "Regular set does not exist"
                decidingSet == null -> "Deciding set does not exist"
                else -> ""
            }
        }

        val newMatchId = singlesMatchRepository.addMatch(tournamentId, matchBody).value

        val shortMatchDto = singlesMatchRepository.getMatchById(newMatchId)

        return shortMatchDto!!.toShortMatchDto()
    }

    suspend fun getMatches(tournamentId: Int): List<ShortMatchDto> {
        return singlesMatchRepository.getMatches(tournamentId).map { matchEntity ->

            val previousSets = if (matchEntity.status == MatchStatus.COMPLETED) {
                singlesMatchLogRepository.getPreviousSets(
                    matchId = matchEntity.id.value,
                    lastPointNumber = Int.MAX_VALUE
                ).map { it.toTennisSetDto() }
            } else emptyList()
            matchEntity.toShortMatchDto(finalScore = previousSets)
        }
    }

    suspend fun getMatchById(matchId: Int): MatchDto {
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

    private suspend fun buildMatchById(matchId: Int, lastPointNumber: Int): MatchDto {
        val matchEntity = singlesMatchRepository.getMatchById(matchId)
            ?: throw NotFoundException("No match found!")

        // для предыдущих партий всегда возвращаем ORDINARY, здесь нам нужен только сам счет
        val previousSets =
            singlesMatchLogRepository.getPreviousSets(matchId, lastPointNumber).map { it.toTennisSetDto() }

        val setNumber = previousSets.size + 1

        val lastPoint = singlesMatchLogRepository.getLastPoint(matchId, lastPointNumber)

        val setsToWin = matchEntity.setsToWin

        val currentSetTemplate = findSetTemplate(matchEntity, setNumber, setsToWin)

        val currentSetMode = calculateCurrentSetMode(currentSetTemplate)

        val currentServe = when {
            lastPoint == null -> matchEntity.firstServe?.id?.value
            else -> lastPoint.currentServe
        }

        val winnerParticipantId = matchEntity.winner?.id?.value
        val firstParticipant = matchEntity.firstParticipant.toParticipantInMatchDto(
            displayName = matchEntity.firstParticipantDisplayName,
            servingParticipantId = currentServe,
            winningParticipantId = winnerParticipantId
        )

        val secondParticipant = matchEntity.secondParticipant.toParticipantInMatchDto(
            displayName = matchEntity.secondParticipantDisplayName,
            servingParticipantId = currentServe,
            winningParticipantId = winnerParticipantId
        )

        val lastGame = singlesMatchLogRepository.getCurrentSet(matchId, setNumber, lastPointNumber)

        val currentSet = when {
            lastPoint?.scoreType == ScoreType.SET -> null
            currentSetMode == SpecialSetMode.SUPER_TIEBREAK -> TennisSetDto(
                firstParticipantGames = lastPoint?.firstParticipantPoints ?: 0,
                secondParticipantGames = lastPoint?.secondParticipantPoints ?: 0,
            )
            // берем счет с последнего гейма, если первый гейм и начат, то выводим 0:0,
            // если первый розыгрыш - то ничего не выводим
            else -> lastGame?.toTennisSetDto() ?: if (lastPoint?.scoreType == ScoreType.POINT) TennisSetDto(
                firstParticipantGames = 0,
                secondParticipantGames = 0,
            ) else null
        }

        val currentGame = when {
            currentSetMode == SpecialSetMode.SUPER_TIEBREAK -> null
            lastPoint?.scoreType in listOf(ScoreType.GAME, ScoreType.SET) -> null
            else -> lastPoint?.toTennisGameDto()
        }

        val matchDto = MatchDto(
            id = matchId.toString(),
            pointShift = matchEntity.pointShift,
            firstParticipant = firstParticipant,
            secondParticipant = secondParticipant,
            status = matchEntity.status,
            previousSets = previousSets,
            currentSet = currentSet,
            currentSetMode = currentSetMode,
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

        if (matchEntity.status != MatchStatus.IN_PROGRESS) {
            throw BadRequestException("Cannot update score. The match is not in progress")
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

        // после сыгранного сета увеличиваем set_number на 1
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
            // EARLY - когда тай-брейк играется при сете 5-5
            //LATE - когда тай-брейк играется при сете 6-6
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
                (firstParticipantPoints + secondParticipantPoints) % 2 == 1 -> calculateNextServe(
                    serveOrder = participantServingOrder,
                    currentServe = currentServe
                )

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
            // для супер-тай-брейка обработка смены подачи ниже, а количество геймов мы не увеличиваем
            scoreType = ScoreType.GAME
            currentServe = calculateNextServe(
                serveOrder = participantServingOrder,
                currentServe = currentServe
            )


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
                    currentSet != null -> calculateNextServe(
                        serveOrder = participantServingOrder,
                        currentServe = currentSet.currentServe!! //currentServe = null ТОЛЬКО после окончания матча
                    )

                    setNumber % 2 == 0 -> secondParticipantToServeInMatch
                    else -> firstParticipantToServeInMatch
                }
            }
        }
        // чтобы не менять currentServe на Int? ради одного частного случая, создадим новую переменную
        var currentServeToInsert: Int? = currentServe

        if (scoreType == ScoreType.SET) {
            val previousSets = singlesMatchLogRepository.getPreviousSets(matchId, lastPointNumber)

            val (firstParticipantPreviousSetsWon, secondParticipantPreviousSetsWon) =
                previousSets.partition { it.firstParticipantPoints > it.secondParticipantPoints }
                    .let { it.first.size to it.second.size }

            val (firstParticipantCurrentSetWon, secondParticipantCurrentSetWon) = when (firstParticipantPoints.compareTo(
                secondParticipantPoints
            )) {
                1 -> 1 to 0
                -1 -> 0 to 1
                else -> 0 to 0
            }

            val firstParticipantSetsWon = firstParticipantPreviousSetsWon + firstParticipantCurrentSetWon
            val secondParticipantSetsWon = secondParticipantPreviousSetsWon + secondParticipantCurrentSetWon


            if (firstParticipantSetsWon == setsToWin || secondParticipantSetsWon == setsToWin) {
                val winnerParticipantId =
                    if (firstParticipantSetsWon == setsToWin) firstParticipantId else secondParticipantId
                singlesMatchRepository.updateWinner(matchId = matchId, winnerParticipantId = winnerParticipantId)
                currentServeToInsert = null
            }
        }

        val singlesMatchLogEvent = SinglesMatchLogEvent(
            matchId = matchId,
            setNumber = setNumber,
            pointNumber = pointNumber,
            scoreType = scoreType,
            currentServe = currentServeToInsert,
            firstParticipantPoints = firstParticipantPoints,
            secondParticipantPoints = secondParticipantPoints
        )

        singlesMatchLogRepository.insertMatchLogEvent(singlesMatchLogEvent)

        val matchDto = buildMatchById(matchId = matchId, lastPointNumber = pointNumber)

        MatchObserver.notifyChange(matchDto)
    }

    private fun findSetTemplate(matchEntity: SinglesMatchEntity, setNumber: Int, setsToWin: Int): SetTemplateEntity {
        val isDecidingSet = setNumber == 2 * setsToWin - 1

        val currentSetTemplate = if (isDecidingSet) {
            matchEntity.decidingSet
        } else {
            // regularSet ВСЕГДА будет проставлен для матчей, где для победы нужно выиграть более 1 сета
            matchEntity.regularSet!!
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


        if (matchEntity.winner != null) {
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

    suspend fun updateMatchStatus(matchId: Int, matchStatusBody: MatchStatusBody) {
        if (matchId == 0) throw BadRequestException("Incorrect id")

        val matchEntity =
            singlesMatchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

        val newStatus = matchStatusBody.status
        validateBody(matchStatusBody) {
            val currentStatus = matchEntity.status

            val winnerParticipantId = matchEntity.winner

            val firstServeParticipant = matchEntity.firstServe

            when {
                (currentStatus == MatchStatus.NOT_STARTED && newStatus == MatchStatus.IN_PROGRESS) -> {
                    if (firstServeParticipant == null) {
                        "Cannot update status to $newStatus: No first serve is set"
                    } else ""
                }

                (currentStatus == MatchStatus.IN_PROGRESS && newStatus == MatchStatus.COMPLETED) -> {
                    if (winnerParticipantId == null) {
                        "Cannot update status to $newStatus: There is no winner in match yet"
                    } else ""
                }

                else -> "Cannot update status from $currentStatus to $newStatus"
            }
        }

        singlesMatchRepository.updateStatus(matchId = matchId, matchStatus = newStatus)


        val matchDto = buildMatchById(matchId, Int.MAX_VALUE)

        MatchObserver.notifyChange(matchDto)
    }
}