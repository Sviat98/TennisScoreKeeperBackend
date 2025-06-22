package com.bashkevich.tennisscorekeeperbackend.model.match.singles

import com.bashkevich.tennisscorekeeperbackend.model.match.MatchStatus
import com.bashkevich.tennisscorekeeperbackend.model.participant.singles.SinglesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateEntity
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentEntity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

data class MatchEntity (
    val id: Int,
    val firstPlayer: PlayerInMatchEntity,
    val secondPlayer: PlayerInMatchEntity,
    val status: MatchStatus,
    val firstPlayerServe: Int?,
    val setsToWin: Int,
    val regularSet: Int,
    val decidingSet: Int,
    val pointShift: Int,
    val winner: Int?
)

class SinglesMatchEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SinglesMatchEntity>(SinglesMatchTable)

    val tournament by TournamentEntity referencedOn SinglesMatchTable.tournament
    val firstParticipant by SinglesParticipantEntity referencedOn SinglesMatchTable.firstParticipant
    var firstParticipantDisplayName by SinglesMatchTable.firstParticipantDisplayName
    val firstParticipantPrimaryColor by SinglesMatchTable.firstParticipantPrimaryColor
    val firstParticipantSecondaryColor by SinglesMatchTable.firstParticipantSecondaryColor
    val secondParticipant by SinglesParticipantEntity referencedOn SinglesMatchTable.secondParticipant
    var secondParticipantDisplayName by SinglesMatchTable.secondParticipantDisplayName
    val secondParticipantPrimaryColor by SinglesMatchTable.secondParticipantPrimaryColor
    val secondParticipantSecondaryColor by SinglesMatchTable.secondParticipantSecondaryColor
    var status by SinglesMatchTable.status
    val firstServingParticipant by SinglesParticipantEntity optionalReferencedOn SinglesMatchTable.firstServingParticipant
    var setsToWin by SinglesMatchTable.setsToWin
    val regularSetTemplate by SetTemplateEntity optionalReferencedOn  SinglesMatchTable.regularSetTemplate
    val decidingSetTemplate by SetTemplateEntity referencedOn SinglesMatchTable.decidingSetTemplate
    var pointShift by SinglesMatchTable.pointShift
    val winnerParticipant by SinglesParticipantEntity optionalReferencedOn SinglesMatchTable.winnerParticipant
    val retiredParticipant by SinglesParticipantEntity optionalReferencedOn SinglesMatchTable.retiredParticipant
}

//data class SinglesMatchEntity (
//    val id: Int,
//    val firstPlayer: PlayerInMatchEntity,
//    val secondPlayer: PlayerInMatchEntity,
//    val status: MatchStatus,
//    val firstPlayerServe: Int?,
//    val setsToWin: Int,
//    val regularSet: Int,
//    val decidingSet: Int,
//    val pointShift: Int,
//    val winner: Int?
//)

//data class SinglesParticipantEntity(
//
//)

data class PlayerInMatchEntity(
    val id: Int,
    val displayName: String
)