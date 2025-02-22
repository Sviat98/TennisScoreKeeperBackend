package com.bashkevich.tennisscorekeeperbackend.feature.counter

import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery
import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterEntity
import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterObserver

class CounterRepository {

    suspend fun getCounters(): List<CounterEntity> {
        return dbQuery {
            CounterEntity.all().sortedBy { it.id.value }.toList()
        }
    }

    suspend fun getCounterById(id: Int): CounterEntity? {
        return dbQuery {
            CounterEntity.findById(id)
        }
    }

    suspend fun addCounter(counterName: String, counterValue: Int): CounterEntity {
        return dbQuery {
            CounterEntity.new {
                name = counterName
                value = counterValue
            }
        }
    }

    suspend fun changeCounterValue(counterId: Int, counterDelta: Int): CounterEntity? {
        return dbQuery {
            val counter = CounterEntity.findByIdAndUpdate(counterId) {
                it.value += counterDelta
            }

            counter?.let {
                CounterObserver.notifyChange(it)
            }

            counter
        }
    }
}