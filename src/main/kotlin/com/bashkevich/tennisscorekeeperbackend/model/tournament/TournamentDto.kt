package com.bashkevich.tennisscorekeeperbackend.model.tournament

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TournamentDto(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: TournamentType,
    @SerialName("status")
    val status: TournamentStatus,
    @SerialName("sets_to_win")
    val setsToWin: Int,
    @SerialName("regular_set_id")
    val regularSetId: String?,
    @SerialName("deciding_set_id")
    val decidingSetId: String,
    @SerialName("theme_id")
    val themeId: String,
    @SerialName("total_participants")
    val totalParticipants: Int,
    @SerialName("total_matches")
    val totalMatches: Int,
    @SerialName("uncompleted_matches")
    val uncompletedMatches: Int,
)
