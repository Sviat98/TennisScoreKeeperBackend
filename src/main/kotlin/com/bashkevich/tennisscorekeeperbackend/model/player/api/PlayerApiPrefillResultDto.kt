package com.bashkevich.tennisscorekeeperbackend.model.player.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Итог предзаполнения player_api_relation (сопоставление игроков нашей системы с игроками API Лиги тенниса). */
@Serializable
data class PlayerApiPrefillResultDto(
    /** Созданные связи. */
    @SerialName("created_relations")
    val createdRelations: Int,
    /** Игроки API, пропущенные без единого кандидата по имени и фамилии. */
    @SerialName("skipped_no_match")
    val skippedNoMatch: Int,
    /** Игроки API, пропущенные из-за нескольких кандидатов по имени и фамилии. */
    @SerialName("skipped_ambiguous")
    val skippedAmbiguous: Int,
    /** Игроки API, у которых связь уже была. */
    @SerialName("already_linked")
    val alreadyLinked: Int,
)
