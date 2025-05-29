package com.bashkevich.tennisscorekeeperbackend.model.counter

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable


object CounterTable: IntIdTable("counter"){
    val name = varchar("name", 50)
    val value = integer("value")
}