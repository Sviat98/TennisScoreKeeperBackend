package com.bashkevich.tennisscorekeeperbackend.model.tournament

import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateEntity
import com.bashkevich.tennisscorekeeperbackend.model.theme.ThemeEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass


class TournamentEntity (id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TournamentEntity>(TournamentTable)

    var name by TournamentTable.name
    var type by TournamentTable.type
    var status by TournamentTable.status
    var setsToWin by TournamentTable.setsToWin
    var regularSetTemplate by SetTemplateEntity optionalReferencedOn TournamentTable.regularSetTemplate
    var decidingSetTemplate by SetTemplateEntity referencedOn TournamentTable.decidingSetTemplate
    var theme by ThemeEntity referencedOn TournamentTable.theme
}