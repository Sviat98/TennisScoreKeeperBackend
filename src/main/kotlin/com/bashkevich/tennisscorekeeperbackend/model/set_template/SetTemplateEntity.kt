package com.bashkevich.tennisscorekeeperbackend.model.set_template

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

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