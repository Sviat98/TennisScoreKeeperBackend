package com.bashkevich.tennisscorekeeperbackend.model.participant.singles

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class SinglesParticipantEntity (id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SinglesParticipantEntity>(SinglesParticipantTable)

    val player by PlayerEntity referencedOn SinglesParticipantTable.player
    var seed  by SinglesParticipantTable.seed
}