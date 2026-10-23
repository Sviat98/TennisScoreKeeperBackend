package com.bashkevich.tennisscorekeeperbackend.model.participant.api

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerEntity

/** Пара участников списка регистрации из API Лиги тенниса с её рейтингом: рейтинг нужен только для сортировки при загрузке. */
data class DoublesParticipantApi(
    val firstPlayer: PlayerEntity,
    val secondPlayer: PlayerEntity,
    val rating: Int,
)
