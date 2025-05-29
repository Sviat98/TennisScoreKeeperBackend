package com.bashkevich.tennisscorekeeperbackend.feature.set_template

import com.bashkevich.tennisscorekeeperbackend.model.set_template.SetTemplateEntity


class SetTemplateRepository {
    fun getSetTemplateById(id: Int): SetTemplateEntity? = SetTemplateEntity.findById(id)
}