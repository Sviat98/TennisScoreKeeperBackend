package com.bashkevich.tennisscorekeeperbackend.model.tournament

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass


class TournamentEntity (id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TournamentEntity>(TournamentTable)

    var name by TournamentTable.name
    var type by TournamentTable.type
    var status by TournamentTable.status
}