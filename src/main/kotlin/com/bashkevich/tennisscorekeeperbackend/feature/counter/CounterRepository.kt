package com.bashkevich.tennisscorekeeperbackend.feature.counter

import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery
import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterEntity

class CounterRepository {

    suspend fun getCounters(): List<CounterEntity> {
        return dbQuery {
            CounterEntity.all().toList()
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
            CounterEntity.findByIdAndUpdate(counterId) {
                it.value += counterDelta
            }
        }
    }
}