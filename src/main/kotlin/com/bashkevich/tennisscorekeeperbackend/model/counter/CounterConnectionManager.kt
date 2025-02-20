package com.bashkevich.tennisscorekeeperbackend.model.counter

import io.ktor.server.websocket.DefaultWebSocketServerSession
import java.util.concurrent.ConcurrentHashMap

object CounterConnectionManager {
    private val connections = ConcurrentHashMap<Int, MutableList<DefaultWebSocketServerSession>>()

    fun addConnection(counterId: Int, session: DefaultWebSocketServerSession) {
        connections.compute(counterId) { _, list ->
            (list ?: mutableListOf()).apply { add(session) }
        }
    }

    fun removeConnection(counterId: Int, session: DefaultWebSocketServerSession) {
        connections[counterId]?.let { list ->
            list.remove(session)
            if (list.isEmpty()) {
                connections.remove(counterId)
            }
        }
    }

    fun getFirstConnection(counterId: Int): DefaultWebSocketServerSession? {
        return connections[counterId]?.firstOrNull()
    }
}