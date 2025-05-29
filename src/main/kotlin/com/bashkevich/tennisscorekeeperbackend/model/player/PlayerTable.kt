package com.bashkevich.tennisscorekeeperbackend.model.player

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.kotlin.datetime.date

object PlayerTable: IntIdTable("player"){
    val surname = varchar("surname", 50)
    val name = varchar("name", 50)
    val dateBirth = date("date_birth")
}
