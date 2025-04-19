package com.bashkevich.tennisscorekeeperbackend.feature.match.websocket

import com.bashkevich.tennisscorekeeperbackend.model.match.MatchDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object MatchObserver {
    private val matchFlows = mutableMapOf<Int, MutableSharedFlow<MatchDto>>()

    fun getMatchFlow(id: Int): SharedFlow<MatchDto> {
        return matchFlows.getOrPut(id) {
            MutableSharedFlow(replay = 1)
        }.asSharedFlow()
    }

    suspend fun notifyChange(match: MatchDto) {
        val id = match.id
        val flow = matchFlows.getOrPut(id) {
            MutableSharedFlow(replay = 1)
        }
        flow.emit(match)
    }
}