package com.bashkevich.tennisscorekeeperbackend.feature.match.set_template

import com.bashkevich.tennisscorekeeperbackend.model.match.MatchTable
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateEntity
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateTable
import org.jetbrains.exposed.sql.selectAll

class SetTemplateRepository {
    fun getSetTemplateById(id: Int) = SetTemplateTable.selectAll().where{
        SetTemplateTable.id eq id
    }.map {
        SetTemplateEntity(
            id = id,
            gamesToWin = it[SetTemplateTable.gamesToWin],
            decidingPoint = it[SetTemplateTable.decidingPoint],
            tiebreakMode = it[SetTemplateTable.tiebreakMode],
            tiebreakPointsToWin = it[SetTemplateTable.tiebreakPointsToWin]
        )
    }.firstOrNull()
}