package com.bashkevich.tennisscorekeeperbackend.feature.match

import com.bashkevich.tennisscorekeeperbackend.feature.match.doubles.DoublesMatchService
import com.bashkevich.tennisscorekeeperbackend.feature.match.singles.SinglesMatchService
import com.bashkevich.tennisscorekeeperbackend.feature.tournament.TournamentRepository
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ChangeScoreBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.body.MatchStatusBody
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ServeBody
import com.bashkevich.tennisscorekeeperbackend.model.match.body.ServeInPairBody
import com.bashkevich.tennisscorekeeperbackend.model.match.ShortMatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.body.RetiredParticipantBody
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentStatus
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentType
import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException

class MatchServiceRouter(
    private val tournamentRepository: TournamentRepository,
    private val singlesMatchService: SinglesMatchService,
    private val doublesMatchService: DoublesMatchService,
) {

    suspend fun addMatch(tournamentId: Int, matchBody: MatchBody): ShortMatchDto {
        return dbQuery {
            if (tournamentId == 0) throw BadRequestException("Wrong format of tournament id")
            val tournament = tournamentRepository.getTournamentById(tournamentId)
                ?: throw NotFoundException("No tournament found by that id")

            if(tournament.status!= TournamentStatus.IN_PROGRESS){
                throw BadRequestException("Cannot add match. Tournament is not in status IN_PROGRESS")
            }

            val shortMatchDto = when (tournament.type){
                TournamentType.SINGLES -> singlesMatchService.addMatch(tournamentId, matchBody)
                TournamentType.DOUBLES -> doublesMatchService.addMatch(tournamentId, matchBody)
                else -> throw IllegalStateException("Unknown tournament type: ${tournament.type}")
            }
            shortMatchDto
        }
    }

    suspend fun getMatchesByTournament(tournamentId: Int): List<ShortMatchDto> {
        return dbQuery {
            if (tournamentId == 0) throw BadRequestException("Wrong format of tournament id")
            val tournament = tournamentRepository.getTournamentById(tournamentId)
                ?: throw NotFoundException("No tournament found by that id")

            val shortMatchListDto = when (tournament.type){
                TournamentType.SINGLES -> singlesMatchService.getMatches(tournamentId)
                TournamentType.DOUBLES -> doublesMatchService.getMatches(tournamentId)
                else -> throw IllegalStateException("Unknown tournament type: ${tournament.type}")
            }
            shortMatchListDto
        }
    }

    suspend fun getMatchById(matchId: Int): MatchDto {
        return dbQuery {
            if (matchId == 0) throw BadRequestException("Wrong format of match id")
            val tournament = tournamentRepository.getTournamentByMatchId(matchId)
                ?: throw NotFoundException("No tournament found by that id")

            val matchDto = when (tournament.type){
                TournamentType.SINGLES -> singlesMatchService.getMatchById(matchId)
                TournamentType.DOUBLES -> doublesMatchService.getMatchById(matchId)
                else -> throw IllegalStateException("Unknown tournament type: ${tournament.type}")
            }
            matchDto
        }
    }

    suspend fun updateServe(matchId: Int, serveBody: ServeBody) {
        dbQuery {
            if (matchId == 0) throw BadRequestException("Wrong format of match id")
            val tournament = tournamentRepository.getTournamentByMatchId(matchId)
                ?: throw NotFoundException("No tournament found by that id")

            when (tournament.type){
                TournamentType.SINGLES -> singlesMatchService.updateServe(matchId,serveBody)
                TournamentType.DOUBLES -> doublesMatchService.updateServe(matchId,serveBody)
                else -> throw IllegalStateException("Unknown tournament type: ${tournament.type}")
            }
        }
    }

    suspend fun updateServeInPair(matchId: Int, serveInPairBody: ServeInPairBody) {
        dbQuery {
            if (matchId == 0) throw BadRequestException("Wrong format of match id")
            val tournament = tournamentRepository.getTournamentByMatchId(matchId)
                ?: throw NotFoundException("No tournament found by that id")

            when (tournament.type){
                TournamentType.SINGLES -> throw BadRequestException("Choosing serve in pair is not allowed in singles matches")
                TournamentType.DOUBLES -> doublesMatchService.updateServeInPair(matchId,serveInPairBody)
                else -> throw IllegalStateException("Unknown tournament type: ${tournament.type}")
            }
        }
    }

    suspend fun updateScore(matchId: Int, changeScoreBody: ChangeScoreBody) {
        dbQuery {
            if (matchId == 0) throw BadRequestException("Wrong format of match id")
            val tournament = tournamentRepository.getTournamentByMatchId(matchId)
                ?: throw NotFoundException("No tournament found by that id")

            when (tournament.type){
                TournamentType.SINGLES -> singlesMatchService.updateScore(matchId,changeScoreBody)
                TournamentType.DOUBLES -> doublesMatchService.updateScore(matchId,changeScoreBody)
                else -> throw IllegalStateException("Unknown tournament type: ${tournament.type}")
            }
        }
    }

    suspend fun undoPoint(matchId: Int) {
        dbQuery {
            if (matchId == 0) throw BadRequestException("Wrong format of match id")
            val tournament = tournamentRepository.getTournamentByMatchId(matchId)
                ?: throw NotFoundException("No tournament found by that id")

            when (tournament.type){
                TournamentType.SINGLES -> singlesMatchService.undoPoint(matchId)
                TournamentType.DOUBLES -> doublesMatchService.undoPoint(matchId)
                else -> throw IllegalStateException("Unknown tournament type: ${tournament.type}")
            }
        }
    }

    suspend fun redoPoint(matchId: Int) {
        dbQuery {
            if (matchId == 0) throw BadRequestException("Wrong format of match id")
            val tournament = tournamentRepository.getTournamentByMatchId(matchId)
                ?: throw NotFoundException("No tournament found by that id")

            when (tournament.type){
                TournamentType.SINGLES -> singlesMatchService.redoPoint(matchId)
                TournamentType.DOUBLES -> doublesMatchService.redoPoint(matchId)
                else -> throw IllegalStateException("Unknown tournament type: ${tournament.type}")
            }
        }
    }

    suspend fun updateMatchStatus(matchId: Int, matchStatusBody: MatchStatusBody) {
        dbQuery {
            if (matchId == 0) throw BadRequestException("Wrong format of match id")
            val tournament = tournamentRepository.getTournamentByMatchId(matchId)
                ?: throw NotFoundException("No tournament found by that id")

            when (tournament.type){
                TournamentType.SINGLES -> singlesMatchService.updateMatchStatus(matchId, matchStatusBody)
                TournamentType.DOUBLES -> doublesMatchService.updateMatchStatus(matchId, matchStatusBody)
                else -> throw IllegalStateException("Unknown tournament type: ${tournament.type}")
            }
        }
    }

    suspend fun setParticipantRetired(matchId: Int, retiredParticipantBody: RetiredParticipantBody) {
        dbQuery {
            if (matchId == 0) throw BadRequestException("Wrong format of match id")
            val tournament = tournamentRepository.getTournamentByMatchId(matchId)
                ?: throw NotFoundException("No tournament found by that id")

            when (tournament.type){
                TournamentType.SINGLES -> singlesMatchService.setParticipantRetired(matchId, retiredParticipantBody)
                TournamentType.DOUBLES -> doublesMatchService.setParticipantRetired(matchId, retiredParticipantBody)
                else -> throw IllegalStateException("Unknown tournament type: ${tournament.type}")
            }
        }
    }


}