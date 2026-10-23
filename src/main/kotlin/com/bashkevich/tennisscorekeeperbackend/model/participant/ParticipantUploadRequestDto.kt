package com.bashkevich.tennisscorekeeperbackend.model.participant

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Тело запроса загрузки участников турнира. seed_amount — количество сеянных участников:
 * первые N участников списка, отсортированного по эло, получают посевы 1..N.
 * Не передан или отрицательный — трактуется как 0 (посевы не выставляются).
 */
@Serializable
data class ParticipantUploadRequestDto(
    @SerialName("seed_amount")
    val seedAmount: Int? = null,
)
