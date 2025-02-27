package com.bashkevich.tennisscorekeeperbackend.model.counter

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object CounterObserver {
    private val counterFlows = mutableMapOf<Int, MutableSharedFlow<CounterEntity>>()

    fun getCounterFlow(id: Int): SharedFlow<CounterEntity> {
        return counterFlows.getOrPut(id) {
            MutableSharedFlow(replay = 1)
        }.asSharedFlow()
    }

    suspend fun notifyChange(counter: CounterEntity) {
        val id = counter.id.value
        val flow = counterFlows.getOrPut(id) {
            MutableSharedFlow(replay = 1)
        }
        flow.emit(counter)
    }
}