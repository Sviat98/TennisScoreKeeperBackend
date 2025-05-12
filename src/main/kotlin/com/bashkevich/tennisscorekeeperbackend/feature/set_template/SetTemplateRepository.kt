package com.bashkevich.tennisscorekeeperbackend.feature.set_template

import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateEntity
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateTable
import org.jetbrains.exposed.sql.selectAll

class SetTemplateRepository {
    fun getSetTemplateById(id: Int): SetTemplateEntity? = SetTemplateEntity.findById(id)
}