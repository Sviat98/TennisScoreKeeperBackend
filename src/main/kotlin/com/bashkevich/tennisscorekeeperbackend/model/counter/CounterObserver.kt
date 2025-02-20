package com.bashkevich.tennisscorekeeperbackend.model.counter

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object CounterObserver {
    private val _counterFlow = MutableSharedFlow<Pair<Int, CounterEntity>>(replay = 1) // Store last state
    val counterFlow = _counterFlow.asSharedFlow()

    suspend fun notifyChange(counter: CounterEntity) {
        _counterFlow.emit(counter.id.value to counter)
    }
}