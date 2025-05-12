package com.bashkevich.tennisscorekeeperbackend.feature.match

import com.bashkevich.tennisscorekeeperbackend.feature.match.doubles.DoublesMatchService
import com.bashkevich.tennisscorekeeperbackend.feature.match.singles.SinglesMatchService
import com.bashkevich.tennisscorekeeperbackend.feature.tournament.TournamentRepository
import com.bashkevich.tennisscorekeeperbackend.model.match.ChangeScoreBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchBody
import com.bashkevich.tennisscorekeeperbackend.model.match.MatchDto
import com.bashkevich.tennisscorekeeperbackend.model.match.ServeBody
import com.bashkevich.tennisscorekeeperbackend.model.match.ShortMatchDto
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

            val shortMatchDto = when (tournament.type){
                TournamentType.SINGLES -> singlesMatchService.addMatch(tournamentId, matchBody)
                TournamentType.DOUBLES -> doublesMatchService.addMatch(tournamentId, matchBody)
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
            }
        }
    }


}