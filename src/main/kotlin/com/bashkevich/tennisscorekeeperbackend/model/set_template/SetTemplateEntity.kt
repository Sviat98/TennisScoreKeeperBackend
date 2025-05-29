package com.bashkevich.tennisscorekeeperbackend.model.set_template

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass


class SetTemplateEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SetTemplateEntity>(SetTemplateTable)

    var name by SetTemplateTable.name
    var gamesToWin by SetTemplateTable.gamesToWin
    var decidingPoint by SetTemplateTable.decidingPoint
    var tiebreakMode by SetTemplateTable.tiebreakMode
    var tiebreakPointsToWin by SetTemplateTable.tiebreakPointsToWin
    var isRegularSet by SetTemplateTable.isRegularSet
    var isDecidingSet by SetTemplateTable.isDecidingSet
}