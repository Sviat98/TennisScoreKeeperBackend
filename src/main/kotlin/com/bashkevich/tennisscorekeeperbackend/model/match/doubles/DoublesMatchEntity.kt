package com.bashkevich.tennisscorekeeperbackend.model.match.doubles

import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantEntity
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerEntity
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateEntity
import com.bashkevich.tennisscorekeeperbackend.model.tournament.TournamentEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass


class DoublesMatchEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DoublesMatchEntity>(DoublesMatchTable)

    val tournament by TournamentEntity referencedOn  DoublesMatchTable.tournament
    val firstParticipant by DoublesParticipantEntity referencedOn DoublesMatchTable.firstParticipant
    var firstParticipantDisplayName by DoublesMatchTable.firstParticipantDisplayName
    val firstParticipantPrimaryColor by DoublesMatchTable.firstParticipantPrimaryColor
    val firstParticipantSecondaryColor by DoublesMatchTable.firstParticipantSecondaryColor
    val secondParticipant by DoublesParticipantEntity referencedOn DoublesMatchTable.secondParticipant
    var secondParticipantDisplayName by DoublesMatchTable.secondParticipantDisplayName
    val secondParticipantPrimaryColor by DoublesMatchTable.secondParticipantPrimaryColor
    val secondParticipantSecondaryColor by DoublesMatchTable.secondParticipantSecondaryColor
    var status by DoublesMatchTable.status
    val firstServingParticipant by DoublesParticipantEntity optionalReferencedOn DoublesMatchTable.firstServingParticipant
    val firstServingPlayerInFirstParticipant by PlayerEntity optionalReferencedOn DoublesMatchTable.firstServingPlayerInFirstPair
    val firstServingPlayerInSecondParticipant by PlayerEntity optionalReferencedOn DoublesMatchTable.firstServingPlayerInSecondPair
    var setsToWin by DoublesMatchTable.setsToWin
    val regularSetTemplate by SetTemplateEntity optionalReferencedOn  DoublesMatchTable.regularSetTemplate
    val decidingSetTemplate by SetTemplateEntity referencedOn DoublesMatchTable.decidingSetTemplate
    var videoLink by DoublesMatchTable.videoLink
    var pointShift by DoublesMatchTable.pointShift
    val winnerParticipant by DoublesParticipantEntity optionalReferencedOn  DoublesMatchTable.winnerParticipant
    val retiredParticipant by DoublesParticipantEntity optionalReferencedOn  DoublesMatchTable.retiredParticipant
}