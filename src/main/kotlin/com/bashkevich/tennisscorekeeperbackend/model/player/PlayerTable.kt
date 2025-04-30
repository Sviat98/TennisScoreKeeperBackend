package com.bashkevich.tennisscorekeeperbackend.model.player

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.alias

object PlayerTable: IntIdTable("player"){
    val surname = varchar("surname", 50)
    val name = varchar("name", 50)
}

// Создаем псевдонимы для таблицы
val FirstPlayer = PlayerTable.alias("first_player")
val SecondPlayer = PlayerTable.alias("second_player")
