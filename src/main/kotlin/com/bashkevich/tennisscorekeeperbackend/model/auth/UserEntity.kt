package com.bashkevich.tennisscorekeeperbackend.model.auth

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass


class UserEntity (id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserEntity>(UserTable)

    var surname  by UserTable.surname
    var name by UserTable.name
    var dateBirth by UserTable.dateBirth
}
