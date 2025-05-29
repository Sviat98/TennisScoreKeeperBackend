package com.bashkevich.tennisscorekeeperbackend.model.participant.doubles

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerExcel

data class DoublesParticipantExcel(
    val firstPlayer: PlayerExcel,
    val secondPlayer: PlayerExcel,
    val rating: Int
)
