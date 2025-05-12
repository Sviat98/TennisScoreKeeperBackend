package com.bashkevich.tennisscorekeeperbackend.model.tournament

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class TournamentEntity (id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TournamentEntity>(TournamentTable)

    var name by TournamentTable.name
    var type by TournamentTable.type
    var status by TournamentTable.status
}