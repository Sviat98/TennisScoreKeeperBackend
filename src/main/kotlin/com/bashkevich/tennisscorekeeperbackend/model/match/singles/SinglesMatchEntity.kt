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
    val secondParticipant by SinglesParticipantEntity referencedOn SinglesMatchTable.secondParticipant
    var secondParticipantDisplayName by SinglesMatchTable.secondParticipantDisplayName
    var status by SinglesMatchTable.status
    val firstServe by SinglesParticipantEntity optionalReferencedOn SinglesMatchTable.firstServe
    var setsToWin by SinglesMatchTable.setsToWin
    val regularSet by SetTemplateEntity optionalReferencedOn  SinglesMatchTable.regularSet
    val decidingSet by SetTemplateEntity referencedOn SinglesMatchTable.decidingSet
    var pointShift by SinglesMatchTable.pointShift
    val winner by SinglesParticipantEntity optionalReferencedOn SinglesMatchTable.winner
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