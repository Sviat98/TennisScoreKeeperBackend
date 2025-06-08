package com.bashkevich.tennisscorekeeperbackend.feature.set_template

import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateEntity
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateTable
import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateTypeFilter
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll


class SetTemplateRepository {
    fun getSetTemplateList(filter: SetTemplateTypeFilter): List<SetTemplateEntity> {
        val query = SetTemplateTable.selectAll()

        when (filter) {
            SetTemplateTypeFilter.REGULAR -> {
                query.andWhere { SetTemplateTable.isRegularSet eq true }
            }
            SetTemplateTypeFilter.DECIDER -> {
                query.andWhere { SetTemplateTable.isDecidingSet eq true }
            }
            else -> {}
        }

        query.orderBy(SetTemplateTable.id)

        return SetTemplateEntity.wrapRows(query).toList()
    }

    fun getSetTemplateById(id: Int): SetTemplateEntity? = SetTemplateEntity.findById(id)
}