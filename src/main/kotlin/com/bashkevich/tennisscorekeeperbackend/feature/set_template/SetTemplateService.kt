package com.bashkevich.tennisscorekeeperbackend.feature.set_template

import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateDto
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateTypeFilter
import com.bashkevich.tennisscorekeeperbackend.model.set_template.toDto
import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery

class SetTemplateService(
    private val setTemplateRepository: SetTemplateRepository
){
    suspend fun getSetTemplates(filter: SetTemplateTypeFilter): List<SetTemplateDto>{
        return dbQuery {
            val setTemplates = setTemplateRepository.getSetTemplateList(filter).map { it.toDto() }

            setTemplates
        }
    }
}