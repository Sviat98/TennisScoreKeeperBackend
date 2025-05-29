package com.bashkevich.tennisscorekeeperbackend.model.player

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.date


object PlayerTable: IntIdTable("player"){
    val surname = varchar("surname", 50)
    val name = varchar("name", 50)
    val dateBirth = date("date_birth")
}
