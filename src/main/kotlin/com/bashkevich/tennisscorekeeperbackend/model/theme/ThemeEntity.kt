package com.bashkevich.tennisscorekeeperbackend.model.theme

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class ThemeEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ThemeEntity>(ThemeTable)

    var name by ThemeTable.name
    var content by ThemeTable.content
}
