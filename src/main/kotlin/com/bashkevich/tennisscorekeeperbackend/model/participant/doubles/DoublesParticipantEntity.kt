package com.bashkevich.tennisscorekeeperbackend.model.participant.doubles

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerEntity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class DoublesParticipantEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DoublesParticipantEntity>(DoublesParticipantTable)

    val firstPlayer by PlayerEntity referencedOn DoublesParticipantTable.firstPlayer
    val secondPlayer by PlayerEntity referencedOn DoublesParticipantTable.secondPlayer
    var seed by DoublesParticipantTable.seed
}