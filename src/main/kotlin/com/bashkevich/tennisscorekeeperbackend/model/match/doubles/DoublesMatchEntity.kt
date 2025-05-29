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
    val secondParticipant by DoublesParticipantEntity referencedOn DoublesMatchTable.secondParticipant
    var secondParticipantDisplayName by DoublesMatchTable.secondParticipantDisplayName
    var status by DoublesMatchTable.status
    val firstServe by DoublesParticipantEntity optionalReferencedOn DoublesMatchTable.firstServe
    val firstParticipantFirstServe by PlayerEntity optionalReferencedOn DoublesMatchTable.firstServeInFirstPair
    val secondParticipantFirstServe by PlayerEntity optionalReferencedOn DoublesMatchTable.firstServeInSecondPair
    var setsToWin by DoublesMatchTable.setsToWin
    val regularSet by SetTemplateEntity referencedOn  DoublesMatchTable.regularSet
    val decidingSet by SetTemplateEntity referencedOn DoublesMatchTable.decidingSet
    var pointShift by DoublesMatchTable.pointShift
    val winner by DoublesParticipantEntity optionalReferencedOn  DoublesMatchTable.winner
}