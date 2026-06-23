package com.bashkevich.tennisscorekeeperbackend.model.tournament

import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateTable
import com.bashkevich.tennisscorekeeperbackend.model.theme.ThemeTable
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable


object TournamentTable : IntIdTable("tournament") {
    val name = varchar("name",50)
    val type = enumerationByName<TournamentType>("type",50)
    val status = enumerationByName<TournamentStatus>("status",50)
    val setsToWin = integer("sets_to_win")
    val regularSetTemplate = reference("regular_set_id", SetTemplateTable).nullable()
    val decidingSetTemplate = reference("deciding_set_id", SetTemplateTable)
    val theme = reference("theme_id", ThemeTable)
}

enum class TournamentType{
    SINGLES,DOUBLES
}

enum class TournamentStatus{
    NOT_STARTED,IN_PROGRESS,COMPLETED
}