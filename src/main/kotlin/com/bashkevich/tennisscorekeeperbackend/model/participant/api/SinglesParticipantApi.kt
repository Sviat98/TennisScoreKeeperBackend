package com.bashkevich.tennisscorekeeperbackend.model.participant.api

import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerEntity

/** Участник списка регистрации из API Лиги тенниса с его рейтингом (эло): рейтинг нужен только для сортировки при загрузке. */
data class SinglesParticipantApi(
    val player: PlayerEntity,
    val rating: Int,
)
