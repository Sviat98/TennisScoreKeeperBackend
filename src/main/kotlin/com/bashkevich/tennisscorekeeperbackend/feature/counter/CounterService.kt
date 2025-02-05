package com.bashkevich.tennisscorekeeperbackend.feature.counter

import com.bashkevich.tennisscorekeeperbackend.plugins.dbQuery
import com.bashkevich.tennisscorekeeperbackend.model.counter.CounterDto
import com.bashkevich.tennisscorekeeperbackend.model.counter.toDto
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException

class CounterService(
    private val counterRepository: CounterRepository,
) {
    suspend fun getCounters(): List<CounterDto> {
        return dbQuery {
            counterRepository.getCounters().map { it.toDto() }
        }
    }

    suspend fun getCounterById(counterId: Int): CounterDto {
        return dbQuery {
            if (counterId != 0) {
                counterRepository.getCounterById(counterId)?.toDto()
                    ?: throw NotFoundException("No counter found!")
            } else throw BadRequestException("Incorrect id")
        }
    }

    suspend fun addCounter(counterName: String): CounterDto {
        return dbQuery {
            counterRepository.addCounter(counterName).toDto()
        }
    }
}