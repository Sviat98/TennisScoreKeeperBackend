package com.bashkevich.tennisscorekeeperbackend.model.counter

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass


class CounterEntity (id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CounterEntity>(CounterTable)

    var name by CounterTable.name
    var value  by CounterTable.value
}