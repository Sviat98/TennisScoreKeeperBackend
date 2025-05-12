package com.bashkevich.tennisscorekeeperbackend.model.tournament

import org.jetbrains.exposed.dao.id.IntIdTable

object TournamentTable : IntIdTable("tournament") {
    val name = varchar("name",50)
    val type = enumerationByName<TournamentType>("type",50)
    val status = enumerationByName<TournamentStatus>("status",50)
}

enum class TournamentType{
    SINGLES,DOUBLES
}

enum class TournamentStatus{
    NOT_STARTED,IN_PROGRESS,COMPLETED
}