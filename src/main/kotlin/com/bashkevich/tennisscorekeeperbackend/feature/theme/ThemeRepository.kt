package com.bashkevich.tennisscorekeeperbackend.feature.theme

import com.bashkevich.tennisscorekeeperbackend.model.theme.ThemeContent
import com.bashkevich.tennisscorekeeperbackend.model.theme.ThemeEntity

class ThemeRepository {
    fun getAll(): List<ThemeEntity> = ThemeEntity.all().sortedBy { it.id.value }

    fun getById(id: Int): ThemeEntity? = ThemeEntity.findById(id)

    fun create(name: String, content: ThemeContent): ThemeEntity = ThemeEntity.new {
        this.name = name
        this.content = content
    }

    fun update(id: Int, name: String, content: ThemeContent): ThemeEntity? {
        return ThemeEntity.findByIdAndUpdate(id) {
            it.name = name
            it.content = content
        }
    }
}
