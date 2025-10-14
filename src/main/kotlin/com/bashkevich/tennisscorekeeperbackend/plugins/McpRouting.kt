package com.bashkevich.tennisscorekeeperbackend.plugins

import com.bashkevich.tennisscorekeeperbackend.feature.match.MatchServiceRouter
import com.bashkevich.tennisscorekeeperbackend.feature.tournament.TournamentService
import io.ktor.server.application.Application
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.koin.ktor.ext.inject

fun Application.configureMcpRouting() {
    mcp {
        val tournamentService: TournamentService by inject()
        val matchServiceRouter: MatchServiceRouter by inject()

        Server(
            serverInfo = Implementation(
                name = "tennis-score-keeper-mcp-server",
                version = "0.0.1",
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = false))
            )
        ).apply {
            addTool(
                name = "list_tournaments",
                description = "получить список всех доступных турниров"
            ) {
                val tournaments = tournamentService.getTournaments()

                val jsonTournaments = Json.encodeToString(tournaments)
                CallToolResult(content = listOf(TextContent(jsonTournaments)))
            }
            addTool(
                name = "get_matches_by_tournament_id",
                description = "получить список всех матчей по id турнира",
                inputSchema = Tool.Input(
                    properties = buildJsonObject {
                        putJsonObject("id") {
                            put("type", "string")
                            put("description", "id турнира")
                        }
                    },
                    required = listOf("id")
                )
            ) { request ->
                val tournamentId = (request.arguments["id"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                val matches = matchServiceRouter.getMatchesByTournament(tournamentId)

                val jsonMatches = Json.encodeToString(matches)
                CallToolResult(content = listOf(TextContent(jsonMatches)))
            }
        }
    }
}