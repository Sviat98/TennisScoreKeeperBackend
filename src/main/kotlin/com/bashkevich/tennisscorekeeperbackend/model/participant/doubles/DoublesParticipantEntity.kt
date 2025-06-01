package com.bashkevich.tennisscorekeeperbackend.model.participant.doubles

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass


class DoublesParticipantEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DoublesParticipantEntity>(DoublesParticipantTable)

    val firstPlayer by PlayerEntity referencedOn DoublesParticipantTable.firstPlayer
    val secondPlayer by PlayerEntity referencedOn DoublesParticipantTable.secondPlayer
    var seed by DoublesParticipantTable.seed
    var saveOrderAtDisplay by DoublesParticipantTable.saveOrderAtDisplay
}