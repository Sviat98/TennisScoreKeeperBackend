package com.bashkevich.tennisscorekeeperbackend.feature.match.doubles

import com.bashkevich.tennisscorekeeperbackend.feature.match.websocket.MatchObserver
import com.bashkevich.tennisscorekeeperbackend.feature.match_log.DoublesMatchLogRepository
import com.bashkevich.tennisscorekeeperbackend.feature.participant.doubles.DoublesParticipantRepository
import com.bashkevich.tennisscorekeeperbackend.feature.set_template.SetTemplateRepository
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ChangeScoreBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchStatus
import com.bashkevich.tennisscorekeeperbackend.model.match.body.MatchStatusBody
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ScoreType
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ServeBody
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ServeInPairBody
import com.bashkevich.tennisscorekeeperbackend.model.match.ShortMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.SpecialSetMode
import com.bashkevich.tennisscorekeeperbackend.model.match.TennisGameDto
import com.bashkevich.tennisscorekeeperbackend.model.match.TennisSetDto
import com.bashkevich.tennisscorekeeperbackend.model.match.body.RetiredParticipantBody
import com.bashkevich.tennisscorekeeperbackend.model.match.doubles.DoublesMatchEntity
import com.bashkevich.tennisscorekeeperbackend.model.match.toShortMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.toTennisGameDto
import com.bashkevich.tennisscorekeeperbackend.model.match.toTennisSetDto
import com.bashkevich.tennisscorekeeperbackend.model.match_log.doubles.DoublesMatchLogEvent
import com.bashkevich.tennisscorekeeperbackend.model.participant.toParticipantInMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateEntity
import com.bashkevich.tennisscorekeeperbackend.model.set_template.TiebreakMode
import com.bashkevich.tennisscorekeeperbackend.plugins.validateRequestConditions
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import kotlin.collections.contains

class DoublesMatchService(
    private val doublesMatchRepository: DoublesMatchRepository,
    private val doublesMatchLogRepository: DoublesMatchLogRepository,
    private val setTemplateRepository: SetTemplateRepository,
    private val doublesParticipantRepository: DoublesParticipantRepository,
) {
    suspend fun addMatch(tournamentId: Int, matchBody: MatchBody): ShortMatchDto {
        validateRequestConditions {
            val firstParticipantId = matchBody.firstParticipant.id.toInt()
            val secondParticipantId = matchBody.secondParticipant.id.toInt()

            val regularSetId = matchBody.regularSet?.toInt() ?: 0
            val decidingSetId = matchBody.decidingSet.toInt()

            val setsToWin = matchBody.setsToWin

            val firstParticipant = doublesParticipantRepository.getParticipantById(firstParticipantId)
            val secondParticipant = doublesParticipantRepository.getParticipantById(secondParticipantId)

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

        val newMatchId = doublesMatchRepository.addMatch(tournamentId, matchBody).value

        val shortMatchDto = doublesMatchRepository.getMatchById(newMatchId)

        return shortMatchDto!!.toShortMatchDto()
    }

    suspend fun getMatches(tournamentId: Int): List<ShortMatchDto> {
        return doublesMatchRepository.getMatches(tournamentId).map { matchEntity ->
            buildShortMatch(matchEntity)
        }
    }

    private suspend fun buildShortMatch(matchEntity: DoublesMatchEntity): ShortMatchDto {
        val matchId = matchEntity.id.value
        return when (matchEntity.status) {
            MatchStatus.PAUSED -> {
                val lastPointInTable = doublesMatchLogRepository.getLastPoint(matchId = matchId)

                val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + matchEntity.pointShift

                val lastPoint =
                    doublesMatchLogRepository.getLastPoint(matchId = matchId, lastPointNumber = lastPointNumber)

                val previousSets =
                    doublesMatchLogRepository.getPreviousSets(matchId, lastPointNumber).map { it.toTennisSetDto() }

                val setNumber = previousSets.size + 1

                val setsToWin = matchEntity.setsToWin

                val currentSetTemplate = findSetTemplate(matchEntity, setNumber, setsToWin)

                val currentSetMode = calculateCurrentSetMode(currentSetTemplate)

                val lastGame = doublesMatchLogRepository.getCurrentSet(matchId, setNumber, lastPointNumber)

                val currentSet = calculateCurrentSetScore(
                    lastPoint = lastPoint, lastGame = lastGame,
                    currentSetMode = currentSetMode
                )

                val currentGame = when {
                    currentSetMode == SpecialSetMode.SUPER_TIEBREAK -> null
                    lastPoint?.scoreType in listOf(
                        ScoreType.GAME, ScoreType.SET, ScoreType.RETIREMENT_FIRST, ScoreType.RETIREMENT_SECOND,
                        ScoreType.FINAL_SET_FIRST, ScoreType.FINAL_SET_SECOND
                    ) -> null

                    else -> lastPoint?.toTennisGameDto()
                }

                matchEntity.toShortMatchDto(previousSets = previousSets, currentSet = currentSet, currentGame = currentGame)
            }

            MatchStatus.COMPLETED -> {

                val previousSets =
                    doublesMatchLogRepository.getPreviousSets(
                        matchId = matchId,
                        lastPointNumber = Int.MAX_VALUE
                    ).map { it.toTennisSetDto() }
                matchEntity.toShortMatchDto(previousSets = previousSets)
            }

            else -> {
                matchEntity.toShortMatchDto()
            }
        }
    }

    suspend fun getMatchById(matchId: Int): MatchDto {
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
        val firstServeParticipantId =
            serveBody.servingParticipantId.toInt() // тут будет Int значение, левые значения строк обработаны в RequestValidation

        val matchEntity = doublesMatchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

        validateRequestConditions {
            when {
                firstServeParticipantId !in listOf(
                    matchEntity.firstParticipant.id.value,
                    matchEntity.secondParticipant.id.value
                ) -> "Serve player id is not in match"

                matchEntity.status != MatchStatus.NOT_STARTED -> "Can't update serve. The match should be in status NOT_STARTED"
                else -> ""
            }
        }

        doublesMatchRepository.updateServe(matchId, firstServeParticipantId)

        val matchDto = buildMatchById(matchId, 0)

        MatchObserver.notifyChange(matchDto)
    }

    suspend fun updateServeInPair(matchId: Int, serveInPairBody: ServeInPairBody) {
        if (matchId == 0) throw BadRequestException("Incorrect id")
        val firstServePlayerId =
            serveInPairBody.servingPlayerId.toInt() // тут будет Int значение, левые значения строк обработаны в RequestValidation

        val matchEntity = doublesMatchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

        val firstParticipant = matchEntity.firstParticipant
        val secondParticipant = matchEntity.secondParticipant

        var isFirstPair = false

        validateRequestConditions {

            when {
                matchEntity.status != MatchStatus.NOT_STARTED -> "Can't update serve in pair. The match should be in status NOT_STARTED"
                else -> {
                    when (firstServePlayerId) {
                        in listOf(
                            firstParticipant.firstPlayer.id.value,
                            firstParticipant.secondPlayer.id.value
                        ),
                            -> {
                            isFirstPair = true
                            ""
                        }

                        in listOf(
                            secondParticipant.firstPlayer.id.value,
                            secondParticipant.secondPlayer.id.value
                        ),
                            -> {
                            isFirstPair = false
                            ""
                        }

                        else -> "Serve player id is not in any of participants"
                    }
                }
            }


        }

        if (isFirstPair) {
            doublesMatchRepository.updateServeInFirstPair(matchId, firstServePlayerId)
        } else {
            doublesMatchRepository.updateServeInSecondPair(matchId, firstServePlayerId)
        }

        val matchDto = buildMatchById(matchId, 0)

        MatchObserver.notifyChange(matchDto)
    }

    private suspend fun buildMatchById(matchId: Int, lastPointNumber: Int): MatchDto {
        val matchEntity = doublesMatchRepository.getMatchById(matchId)
            ?: throw NotFoundException("No match found!")

        // для предыдущих партий всегда возвращаем ORDINARY, здесь нам нужен только сам счет
        val previousSets =
            doublesMatchLogRepository.getPreviousSets(matchId, lastPointNumber).map { it.toTennisSetDto() }

        val lastPoint = doublesMatchLogRepository.getLastPoint(matchId, lastPointNumber)

        val playerServingOrder = buildPlayerServeOrder(matchEntity)

        val firstPlayerToServe = playerServingOrder[0]

        val secondPlayerToServe = playerServingOrder[1]


        // Почему не сделали currentServe = null, если есть победитель?
        // После последнего выигранного розыгрыша serve и так становится равным null, поэтому лишней проверки не нужно
        val currentServe = when {
            lastPoint == null -> matchEntity.firstServingParticipant?.id?.value
            else -> lastPoint.currentServe
        }

        // Почему не сделали currentServe = null, если есть победитель?
        // После последнего выигранного розыгрыша serve и так становится равным null, поэтому лишней проверки не нужно
        val currentPlayerToServe = when {
            lastPoint == null -> firstPlayerToServe
            else -> lastPoint.currentServeInPair
        }

        val nextPlayerToServe = when {
            lastPoint == null -> secondPlayerToServe
            else -> {
                val currentServingPlayerIndex = playerServingOrder.indexOf(lastPoint.currentServeInPair)

                playerServingOrder[(currentServingPlayerIndex + 1) % 4]
            }
        }

        var currentSetMode: SpecialSetMode?
        var currentSet: TennisSetDto?
        var currentGame: TennisGameDto?

        val winnerParticipantId = matchEntity.winnerParticipant?.id?.value

        val retiredParticipantId = matchEntity.retiredParticipant?.id?.value

        if (winnerParticipantId == null) {
            val setNumber = previousSets.size + 1


            val setsToWin = matchEntity.setsToWin

            val currentSetTemplate = findSetTemplate(matchEntity, setNumber, setsToWin)

            currentSetMode = calculateCurrentSetMode(currentSetTemplate)

            val lastGame = doublesMatchLogRepository.getCurrentSet(matchId, setNumber, lastPointNumber)

            currentSet = calculateCurrentSetScore(
                lastPoint = lastPoint, lastGame = lastGame,
                currentSetMode = currentSetMode
            )

            currentGame = when {
                currentSetMode == SpecialSetMode.SUPER_TIEBREAK -> null
                lastPoint?.scoreType in listOf(
                    ScoreType.GAME, ScoreType.SET, ScoreType.RETIREMENT_FIRST, ScoreType.RETIREMENT_SECOND,
                    ScoreType.FINAL_SET_FIRST, ScoreType.FINAL_SET_SECOND
                ) -> null

                else -> lastPoint?.toTennisGameDto()
            }
        } else {
            // если в матче есть победитель, все зануляем (подача также зануляется, но в общем алгоритме)
            currentSetMode = null
            currentSet = null
            currentGame = null
        }

        val firstParticipant = matchEntity.firstParticipant.toParticipantInMatchDto(
            displayName = matchEntity.firstParticipantDisplayName,
            primaryColor = matchEntity.firstParticipantPrimaryColor,
            secondaryColor = matchEntity.firstParticipantSecondaryColor,
            servingParticipantId = currentServe,
            winningParticipantId = winnerParticipantId,
            retiredParticipantId = retiredParticipantId,
            nowServingPlayerId = currentPlayerToServe,
            nextServingPlayerId = nextPlayerToServe
        )

        val secondParticipant = matchEntity.secondParticipant.toParticipantInMatchDto(
            displayName = matchEntity.secondParticipantDisplayName,
            primaryColor = matchEntity.secondParticipantPrimaryColor,
            secondaryColor = matchEntity.secondParticipantSecondaryColor,
            servingParticipantId = currentServe,
            winningParticipantId = winnerParticipantId,
            retiredParticipantId = retiredParticipantId,
            nowServingPlayerId = currentPlayerToServe,
            nextServingPlayerId = nextPlayerToServe
        )

        val matchDto = MatchDto(
            id = matchId.toString(),
            pointShift = matchEntity.pointShift,
            videoLink = matchEntity.videoLink,
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
        val scoringParticipantId =
            changeScoreBody.participantId.toInt() // тут будет Int значение, левые значения строк обработаны в RequestValidation

        val matchEntity = doublesMatchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

        val firstParticipantId = matchEntity.firstParticipant.id.value

        val secondParticipantId = matchEntity.secondParticipant.id.value

        val firstParticipantToServe = matchEntity.firstServingParticipant!!.id.value

        val secondParticipantToServe =
            if (firstParticipantToServe == firstParticipantId) secondParticipantId else firstParticipantId

        val participantServingOrder = listOf(firstParticipantToServe, secondParticipantToServe)

        val playerServingOrder = buildPlayerServeOrder(matchEntity).filterNotNull()

        val firstPlayerToServe = playerServingOrder[0]

        val lastPointInTable = doublesMatchLogRepository.getLastPoint(matchId)

        val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + matchEntity.pointShift

        val lastPoint = doublesMatchLogRepository.getLastPoint(matchId, lastPointNumber)

        validateRequestConditions {
            when {
                scoringParticipantId !in listOf(
                    firstParticipantId,
                    secondParticipantId
                ) -> "Scoring player id is not in match"

                matchEntity.status != MatchStatus.IN_PROGRESS -> "Cannot update score. The match should be in status IN_PROGRESS"
                changeScoreBody.scoreType == ScoreType.GAME && lastPoint?.scoreType == ScoreType.POINT -> "Cannot add game to a score"
                matchEntity.winnerParticipant != null -> "Cannot update score. The match already has the winner"
                else -> ""
            }

        }

        if (matchEntity.pointShift < 0) {
            doublesMatchRepository.updatePointShift(matchId = matchId, newPointShift = 0)

            doublesMatchLogRepository.removeEvents(matchId = matchId, pointNumber = lastPointNumber)
        }

        val setsToWin = matchEntity.setsToWin

        var setNumber = lastPoint?.setNumber ?: 1

        var pointNumber = lastPoint?.pointNumber ?: 0

        pointNumber++

        var currentServe = lastPoint?.currentServe ?: firstParticipantToServe
        var currentPlayerToServe = lastPoint?.currentServeInPair ?: firstPlayerToServe

        var firstParticipantPoints = 0
        var secondParticipantPoints = 0

        var scoreType = changeScoreBody.scoreType

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

        val currentSet = doublesMatchLogRepository.getCurrentSet(matchId, setNumber, lastPointNumber)

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
            currentPlayerToServe = when {
                (firstParticipantPoints + secondParticipantPoints) % 2 == 1 -> calculateNextServe(
                    serveOrder = playerServingOrder,
                    currentServe = currentPlayerToServe
                )

                else -> currentPlayerToServe
            }
        } else {
            isFirstParticipantWonGame = when {
                changeScoreBody.scoreType == ScoreType.GAME -> scoringParticipantId == firstParticipantId
                currentSetTemplate.decidingPoint -> firstParticipantPoints == 4
                else -> (firstParticipantPoints == 4 && secondParticipantPoints < 3) || (firstParticipantPoints > 4 && firstParticipantPoints - secondParticipantPoints == 2)
            }

            isSecondParticipantWonGame = when {
                changeScoreBody.scoreType == ScoreType.GAME -> scoringParticipantId == secondParticipantId
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
            currentPlayerToServe = calculateNextServe(
                serveOrder = playerServingOrder,
                currentServe = currentPlayerToServe
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
                    // берем подачу в первом розыгрыше тай-брейка и меняем ее на противоположную
                    currentSet != null -> calculateNextServe(
                        serveOrder = participantServingOrder,
                        currentServe = currentSet.currentServe!! //currentServe = null ТОЛЬКО после окончания матча
                    )

                    // если завершается первый сет, а геймов не было (к примеру, супер тай-брейк), то в следующем сете подает второй по очереди игрок
                    setNumber % 2 == 0 -> firstParticipantToServe
                    else -> secondParticipantToServe
                }
                currentPlayerToServe = when {
                    currentSet != null -> calculateNextServe(
                        serveOrder = playerServingOrder,
                        currentServe = currentSet.currentServeInPair!! //currentServeInPair = null ТОЛЬКО после окончания матча
                    )

                    else -> {
                        val playerServingIndex =
                            setNumber % 4 // после 1-го сета будет подавать второй игрок, index = 1 и т д
                        playerServingOrder[playerServingIndex]
                    }
                }
            }
        }
        // чтобы не менять currentServe и currentPlayerToServe на Int? ради одного частного случая, создадим новые переменные
        var currentServeToInsert: Int? = currentServe
        var currentServePlayerToInsert: Int? = currentPlayerToServe


        if (scoreType == ScoreType.SET) {
            val previousSets = doublesMatchLogRepository.getPreviousSets(matchId, lastPointNumber)

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
                scoreType =
                    if (winnerParticipantId == firstParticipantId) ScoreType.FINAL_SET_FIRST else ScoreType.FINAL_SET_SECOND
                doublesMatchRepository.updateWinner(matchId = matchId, winnerParticipantId = winnerParticipantId)
                // матч закончился, вставляем currentServe и currentPlayerToServe, равные null
                currentServeToInsert = null
                currentServePlayerToInsert = null
            }
        }

        val doublesMatchLogEvent = DoublesMatchLogEvent(
            matchId = matchId,
            setNumber = setNumber,
            pointNumber = pointNumber,
            scoreType = scoreType,
            currentServe = currentServeToInsert,
            currentServeInPair = currentServePlayerToInsert,
            firstParticipantPoints = firstParticipantPoints,
            secondParticipantPoints = secondParticipantPoints,
        )

        doublesMatchLogRepository.insertMatchLogEvent(doublesMatchLogEvent)

        val matchDto = buildMatchById(matchId = matchId, lastPointNumber = pointNumber)

        MatchObserver.notifyChange(matchDto)
    }

    private fun buildPlayerServeOrder(matchEntity: DoublesMatchEntity): List<Int?> {
        val firstParticipantId = matchEntity.firstParticipant.id.value
        val firstParticipantToServe = matchEntity.firstServingParticipant?.id?.value

        val firstServingPlayerInFirstParticipant = matchEntity.firstServingPlayerInFirstParticipant?.id?.value
        val firstServingPlayerInSecondParticipant = matchEntity.firstServingPlayerInSecondParticipant?.id?.value

        val firstParticipantFirstPlayerId = matchEntity.firstParticipant.firstPlayer.id.value

        val firstParticipantSecondPlayerId = matchEntity.firstParticipant.secondPlayer.id.value

        val secondParticipantFirstPlayerId = matchEntity.secondParticipant.firstPlayer.id.value

        val secondParticipantSecondPlayerId = matchEntity.secondParticipant.secondPlayer.id.value

        val (firstPlayerToServe, thirdPlayerToServe) = if (firstParticipantToServe == firstParticipantId) {
            firstServingPlayerInFirstParticipant to
                    (if (firstServingPlayerInFirstParticipant == firstParticipantFirstPlayerId) firstParticipantSecondPlayerId else firstParticipantFirstPlayerId)
        } else {
            firstServingPlayerInSecondParticipant to
                    (if (firstServingPlayerInSecondParticipant == secondParticipantFirstPlayerId) secondParticipantSecondPlayerId else secondParticipantFirstPlayerId)
        }

        val (secondPlayerToServe, fourthPlayerToServe) = if (firstParticipantToServe == firstParticipantId) {
            firstServingPlayerInSecondParticipant to
                    (if (firstServingPlayerInSecondParticipant == secondParticipantFirstPlayerId) secondParticipantSecondPlayerId else secondParticipantFirstPlayerId)
        } else {
            firstServingPlayerInFirstParticipant to
                    (if (firstServingPlayerInFirstParticipant == firstParticipantFirstPlayerId) firstParticipantSecondPlayerId else firstParticipantFirstPlayerId)
        }

        return listOf(firstPlayerToServe, secondPlayerToServe, thirdPlayerToServe, fourthPlayerToServe)
    }

    private fun findSetTemplate(matchEntity: DoublesMatchEntity, setNumber: Int, setsToWin: Int): SetTemplateEntity {
        val isDecidingSet = setNumber == 2 * setsToWin - 1

        val currentSetTemplate = if (isDecidingSet) {
            matchEntity.decidingSetTemplate
        } else {
            // regularSet ВСЕГДА будет проставлен для матчей, где для победы нужно выиграть более 1 сета
            matchEntity.regularSetTemplate!!
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


    private fun calculateCurrentSetScore(
        lastPoint: DoublesMatchLogEvent?,
        lastGame: DoublesMatchLogEvent?,
        currentSetMode: SpecialSetMode?,
    ): TennisSetDto? {
        return when {
            lastPoint?.scoreType == ScoreType.SET -> null
            // если матч начинается с супер тай-брейка, то lastPoint = null, свалимся в последнюю ветку
            // в противном случае выводим счет супер тай-брейка, как будто это сет
            currentSetMode == SpecialSetMode.SUPER_TIEBREAK -> lastPoint?.let {
                TennisSetDto(
                    firstParticipantGames = lastPoint.firstParticipantPoints,
                    secondParticipantGames = lastPoint.secondParticipantPoints,
                )
            }
            // берем счет с последнего гейма, если первый гейм и начат, то выводим 0:0,
            // если первый розыгрыш - то ничего не выводим
            else -> lastGame?.toTennisSetDto() ?: if (lastPoint?.scoreType == ScoreType.POINT) TennisSetDto(
                firstParticipantGames = 0,
                secondParticipantGames = 0,
            ) else null
        }
    }

    suspend fun setParticipantRetired(matchId: Int, retiredParticipantBody: RetiredParticipantBody) {
        if (matchId == 0) throw BadRequestException("Incorrect id")

        val matchEntity =
            doublesMatchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

        val firstParticipantId = matchEntity.firstParticipant.id.value
        val secondParticipantId = matchEntity.secondParticipant.id.value


        val retiredParticipantId =
            retiredParticipantBody.retiredParticipantId.toInt() // тут будет Int значение, левые значения строк обработаны в RequestValidation

        val lastPointInTable = doublesMatchLogRepository.getLastPoint(matchId)

        validateRequestConditions {
            when {
                retiredParticipantId !in listOf(
                    firstParticipantId,
                    secondParticipantId
                ) -> "Serve player id is not in match"

                matchEntity.status != MatchStatus.PAUSED -> "Cannot retire the participant. The match should be in status PAUSED"
                else -> ""
            }
        }

        val pointShift = matchEntity.pointShift

        val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + pointShift

        if (matchEntity.pointShift < 0) {
            doublesMatchRepository.updatePointShift(matchId = matchId, newPointShift = 0)

            doublesMatchLogRepository.removeEvents(matchId = matchId, pointNumber = lastPointNumber)
        }

        var winnerParticipantId: Int
        var scoreType: ScoreType

        if (retiredParticipantId == firstParticipantId) {
            scoreType = ScoreType.RETIREMENT_FIRST
            winnerParticipantId = secondParticipantId
        } else {
            scoreType = ScoreType.RETIREMENT_SECOND
            winnerParticipantId = firstParticipantId
        }

        // для предыдущих партий всегда возвращаем ORDINARY, здесь нам нужен только сам счет
        val previousSets =
            doublesMatchLogRepository.getPreviousSets(matchId, lastPointNumber).map { it.toTennisSetDto() }

        val setNumber = previousSets.size + 1

        val setsToWin = matchEntity.setsToWin

        val currentSetTemplate = findSetTemplate(matchEntity, setNumber, setsToWin)
        val currentSetMode = calculateCurrentSetMode(currentSetTemplate)

        val lastPoint = doublesMatchLogRepository.getLastPoint(matchId, lastPointNumber)
        val lastGame = doublesMatchLogRepository.getCurrentSet(matchId, setNumber, lastPointNumber)

        val currentSetScore = calculateCurrentSetScore(
            lastPoint = lastPoint, lastGame = lastGame,
            currentSetMode = currentSetMode
        ) ?: TennisSetDto(firstParticipantGames = 0, secondParticipantGames = 0)

        val firstParticipantPoints = currentSetScore.firstParticipantGames
        val secondParticipantPoints = currentSetScore.secondParticipantGames

        val newPointNumber = lastPointNumber + 1

        doublesMatchRepository.setParticipantRetired(matchId = matchId, retiredParticipantId = retiredParticipantId)
        doublesMatchRepository.updateWinner(matchId = matchId, winnerParticipantId = winnerParticipantId)
        doublesMatchRepository.updateStatus(matchId = matchId, matchStatus = MatchStatus.IN_PROGRESS)

        val doublesMatchLogEvent = DoublesMatchLogEvent(
            matchId = matchId,
            setNumber = setNumber,
            pointNumber = newPointNumber,
            scoreType = scoreType,
            currentServe = null,
            currentServeInPair = null,
            firstParticipantPoints = firstParticipantPoints,
            secondParticipantPoints = secondParticipantPoints
        )

        doublesMatchLogRepository.insertMatchLogEvent(doublesMatchLogEvent)

        val matchDto = buildMatchById(matchId = matchId, lastPointNumber = newPointNumber)

        MatchObserver.notifyChange(matchDto)
    }

    suspend fun undoPoint(matchId: Int) {
        if (matchId == 0) throw BadRequestException("Incorrect id")
        val matchEntity =
            doublesMatchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

        val lastPointInTable = doublesMatchLogRepository.getLastPoint(matchId)

        val pointShift = matchEntity.pointShift

        val newPointShift = pointShift - 1

        val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + newPointShift

        validateRequestConditions {
            when {
                lastPointNumber < 0 -> "Cannot undo the point. There are no points behind"
                matchEntity.status != MatchStatus.IN_PROGRESS -> "Cannot undo the point. The match should be in status IN_PROGRESS"
                else -> ""
            }
        }
        matchEntity.winnerParticipant?.let {
            doublesMatchRepository.updateWinner(matchId = matchId, winnerParticipantId = null)

            matchEntity.retiredParticipant?.let {
                doublesMatchRepository.setParticipantRetired(matchId = matchId, retiredParticipantId = null)
            }
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

        validateRequestConditions {
            when {
                pointShift == 0 -> "Cannot redo the point. There are no points ahead"
                matchEntity.status != MatchStatus.IN_PROGRESS -> "Cannot redo the point. The match should be in status IN_PROGRESS"
                else -> ""
            }
        }

        val newPointShift = pointShift + 1

        val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + newPointShift

        doublesMatchRepository.updatePointShift(matchId = matchId, newPointShift = newPointShift)

        val firstParticipantId = matchEntity.firstParticipant.id.value
        val secondParticipantId = matchEntity.secondParticipant.id.value

        var retiredParticipantId: Int
        var winnerParticipantId: Int

        val lastPoint = doublesMatchLogRepository.getLastPoint(matchId, lastPointNumber)

        if (lastPoint?.scoreType in listOf(ScoreType.FINAL_SET_FIRST, ScoreType.FINAL_SET_SECOND)) {
            winnerParticipantId =
                if (lastPoint?.scoreType == ScoreType.FINAL_SET_FIRST) firstParticipantId else secondParticipantId

            doublesMatchRepository.updateWinner(matchId = matchId, winnerParticipantId = winnerParticipantId)
        }

        if (lastPoint?.scoreType in listOf(ScoreType.RETIREMENT_FIRST, ScoreType.RETIREMENT_SECOND)) {

            if (lastPoint?.scoreType == ScoreType.RETIREMENT_FIRST) {
                retiredParticipantId = firstParticipantId
                winnerParticipantId = secondParticipantId
            } else {
                retiredParticipantId = secondParticipantId
                winnerParticipantId = firstParticipantId
            }

            doublesMatchRepository.setParticipantRetired(matchId = matchId, retiredParticipantId = retiredParticipantId)
            doublesMatchRepository.updateWinner(matchId = matchId, winnerParticipantId = winnerParticipantId)
        }

        val matchDto = buildMatchById(matchId, lastPointNumber)

        MatchObserver.notifyChange(matchDto)
    }


    suspend fun updateMatchStatus(matchId: Int, matchStatusBody: MatchStatusBody) {
        if (matchId == 0) throw BadRequestException("Incorrect id")

        val matchEntity =
            doublesMatchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

        val newStatus = matchStatusBody.status
        validateRequestConditions {
            val currentStatus = matchEntity.status

            val winnerParticipantId = matchEntity.winnerParticipant

            val firstServeParticipant = matchEntity.firstServingParticipant
            val firstServeInFirstPair = matchEntity.firstServingPlayerInFirstParticipant
            val firstServeInSecondPair = matchEntity.firstServingPlayerInSecondParticipant

            when {
                currentStatus == newStatus -> ""
                (currentStatus == MatchStatus.NOT_STARTED && newStatus == MatchStatus.IN_PROGRESS) -> {
                    when {
                        firstServeParticipant == null -> "Cannot update status to $newStatus: No first serve is set"
                        firstServeInFirstPair == null -> "Cannot update status to $newStatus: No first serve in first pair is set"
                        firstServeInSecondPair == null -> "Cannot update status to $newStatus: No first serve in second pair is set"
                        else -> ""
                    }
                }

                (currentStatus == MatchStatus.IN_PROGRESS && newStatus == MatchStatus.PAUSED) -> {
                    if (winnerParticipantId != null) {
                        "Cannot update status to $newStatus: There is already a winner"
                    } else ""
                }

                (currentStatus == MatchStatus.PAUSED && newStatus == MatchStatus.IN_PROGRESS) -> ""
                (currentStatus == MatchStatus.IN_PROGRESS && newStatus == MatchStatus.COMPLETED) -> {
                    if (winnerParticipantId == null) {
                        "Cannot update status to $newStatus: There is no winner in match yet"
                    } else ""
                }

                else -> "Cannot update status from $currentStatus to $newStatus"
            }
        }

        val lastPointInTable = doublesMatchLogRepository.getLastPoint(matchId)

        val pointShift = matchEntity.pointShift

        val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + pointShift

        doublesMatchRepository.updateStatus(matchId = matchId, matchStatus = newStatus)

        val matchDto = buildMatchById(matchId = matchId, lastPointNumber = lastPointNumber)

        MatchObserver.notifyChange(matchDto)
    }

    suspend fun setVideoLink(matchId: Int, videoLink: String) {
        if (matchId == 0) throw BadRequestException("Incorrect id")

        val matchEntity =
            doublesMatchRepository.getMatchById(matchId) ?: throw NotFoundException("No match found!")

        val lastPointInTable = doublesMatchLogRepository.getLastPoint(matchId)

        val pointShift = matchEntity.pointShift

        val lastPointNumber = (lastPointInTable?.pointNumber ?: 0) + pointShift

        doublesMatchRepository.updateVideoLink(matchId,videoLink)

        val matchDto = buildMatchById(matchId = matchId, lastPointNumber = lastPointNumber)

        MatchObserver.notifyChange(matchDto)
    }
}