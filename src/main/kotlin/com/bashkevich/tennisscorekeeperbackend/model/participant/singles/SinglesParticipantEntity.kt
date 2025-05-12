package com.bashkevich.tennisscorekeeperbackend.model.participant.singles

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerEntity
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SinglesParticipantEntity (id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SinglesParticipantEntity>(SinglesParticipantTable)

    val player by PlayerEntity referencedOn SinglesParticipantTable.player
    var seed  by SinglesParticipantTable.seed
}