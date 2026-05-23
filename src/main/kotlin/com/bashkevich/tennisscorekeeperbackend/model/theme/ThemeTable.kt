package com.bashkevich.tennisscorekeeperbackend.model.theme

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.json.jsonb

object ThemeTable : IntIdTable("theme") {
    val name = varchar("name", 150)
    val content = jsonb<ThemeContent>("content", Json.Default)
}
