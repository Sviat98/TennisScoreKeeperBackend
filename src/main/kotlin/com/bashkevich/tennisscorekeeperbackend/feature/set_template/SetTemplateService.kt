package com.bashkevich.tennisscorekeeperbackend.feature.set_template

import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateDto
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateTypeFilter
import com.bashkevich.tennisscorekeeperbackend.model.set_template.toDto
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
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

    suspend fun getSetTemplateById(id: Int): SetTemplateDto {
        return dbQuery {
            if (id == 0) throw BadRequestException("Wrong format of set template id")

            setTemplateRepository.getSetTemplateById(id)?.toDto()
                ?: throw NotFoundException("No set template found by that id")
        }
    }
}