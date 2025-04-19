package com.bashkevich.tennisscorekeeperbackend.model.player

import org.jetbrains.exposed.dao.id.IntIdTable

object PlayerTable: IntIdTable("player"){
    val surname = varchar("surname", 50)
    val name = varchar("name", 50)
}