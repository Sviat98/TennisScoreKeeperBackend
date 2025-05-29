package com.bashkevich.tennisscorekeeperbackend.model.player

import kotlinx.datetime.LocalDate


data class PlayerExcel(
    val name: String,
    val surname: String,
    val dateBirth: LocalDate
)
