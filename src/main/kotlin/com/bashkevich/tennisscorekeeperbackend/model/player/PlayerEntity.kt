package com.bashkevich.tennisscorekeeperbackend.model.player

import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class PlayerEntity (id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerEntity>(PlayerTable)

    var surname  by PlayerTable.surname
    var name by PlayerTable.name
}