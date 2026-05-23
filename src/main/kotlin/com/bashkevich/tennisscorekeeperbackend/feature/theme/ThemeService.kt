package com.bashkevich.tennisscorekeeperbackend.feature.theme

import com.bashkevich.tennisscorekeeperbackend.model.theme.ThemeContent
import com.bashkevich.tennisscorekeeperbackend.model.theme.ThemeDto
import com.bashkevich.tennisscorekeeperbackend.model.theme.toDto
import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException

class ThemeService(
    private val themeRepository: ThemeRepository
) {
    suspend fun getThemes(): List<ThemeDto> {
        return dbQuery {
            themeRepository.getAll().map { it.toDto() }
        }
    }

    suspend fun getThemeById(themeId: Int): ThemeDto {
        return dbQuery {
            if (themeId == 0) throw BadRequestException("Incorrect id")
            themeRepository.getById(themeId)?.toDto()
                ?: throw NotFoundException("No theme found!")
        }
    }

    suspend fun createTheme(name: String, content: ThemeContent): ThemeDto {
        return dbQuery {
            themeRepository.create(name, content).toDto()
        }
    }

    suspend fun updateTheme(id: Int, name: String, content: ThemeContent): ThemeDto {
        return dbQuery {
            if (id == 0) throw BadRequestException("Incorrect id")
            themeRepository.update(id, name, content)?.toDto()
                ?: throw NotFoundException("No theme found!")
        }
    }
}
