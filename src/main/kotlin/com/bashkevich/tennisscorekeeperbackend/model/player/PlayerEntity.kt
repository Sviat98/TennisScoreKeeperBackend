package com.bashkevich.tennisscorekeeperbackend.model.player

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class PlayerEntity (id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerEntity>(PlayerTable)

    var surname  by PlayerTable.surname
    var name by PlayerTable.name
    var dateBirth by PlayerTable.dateBirth
}