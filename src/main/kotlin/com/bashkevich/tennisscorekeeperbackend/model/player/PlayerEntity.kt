package com.bashkevich.tennisscorekeeperbackend.model.player

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass


class PlayerEntity (id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerEntity>(PlayerTable)

    var surname  by PlayerTable.surname
    var name by PlayerTable.name
    var dateBirth by PlayerTable.dateBirth
}