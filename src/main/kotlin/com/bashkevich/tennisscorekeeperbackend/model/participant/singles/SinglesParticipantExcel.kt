package com.bashkevich.tennisscorekeeperbackend.model.participant.singles

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerExcel

data class SinglesParticipantExcel(
    val player: PlayerExcel,
    val rating: Int
)
