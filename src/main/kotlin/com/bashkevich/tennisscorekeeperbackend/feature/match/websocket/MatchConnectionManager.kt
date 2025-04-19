package com.bashkevich.tennisscorekeeperbackend.feature.match.websocket

import io.ktor.server.websocket.DefaultWebSocketServerSession
import java.util.concurrent.ConcurrentHashMap

object MatchConnectionManager {
    private val connections = ConcurrentHashMap<Int, MutableList<DefaultWebSocketServerSession>>()

    fun addConnection(matchId: Int, session: DefaultWebSocketServerSession) {
        connections.compute(matchId) { _, list ->
            (list ?: mutableListOf()).apply { add(session) }
        }
        println("Connection added. There are ${connections.size} active connections for matchId = $matchId")
    }

    fun removeConnection(matchId: Int, session: DefaultWebSocketServerSession) {
        connections[matchId]?.let { list ->
            list.remove(session)
            if (list.isEmpty()) {
                connections.remove(matchId)
            }
            println("Connection removed. There are ${connections.size} active connections for matchId = $matchId")
        }
    }

    fun getFirstConnection(matchId: Int): DefaultWebSocketServerSession? {
        return connections[matchId]?.firstOrNull()
    }
}