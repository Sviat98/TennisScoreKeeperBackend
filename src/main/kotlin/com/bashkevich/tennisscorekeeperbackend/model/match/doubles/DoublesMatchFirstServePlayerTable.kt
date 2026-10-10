package com.bashkevich.tennisscorekeeperbackend.model.match.doubles

import com.bashkevich.tennisscorekeeperbackend.model.participant.doubles.DoublesParticipantTable
import com.bashkevich.tennisscorekeeperbackend.model.player.PlayerTable
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable

object DoublesMatchFirstServePlayerTable : CompositeIdTable("doubles_match_first_serve_player") {
    val match = reference("match_id", DoublesMatchTable)
    val participant = reference("participant_id", DoublesParticipantTable)
    val set = integer("set_number").entityId()

    // какая пара подает первой в этом сете: 1 или 2; всегда заполняется при создании записи
    val serveOrder = integer("serve_order")
    val player = reference("player_id", PlayerTable)

    // граница redo в doubles_match_log: redo доступен до этого номера включительно.
    // Следующая за ней строка (limit + 1) - первая строка сета, "засвечивающая" подачу
    // пары, - сверяется isRedoBlockedByServeChange с очередью; ее тип задает механику
    // сверки (GAME или TIEBREAK_POINT у первой пары - партнер первого подающего,
    // совпадение; POINT/TIEBREAK_POINT у второй пары - первый подающий, несовпадение).
    // В супер-тай-брейке граница второй пары - сам первый розыгрыш (его проекция несет
    // ее первого подающего на второй розыгрыш и мутируется при смене подачи на границе).
    // Чистится при усечении хвоста
    val pointNumberRedoLimit = integer("point_number_redo_limit").nullable()

    init {
        addIdColumn(match)
        addIdColumn(participant)
    }

    override val primaryKey = PrimaryKey(match, participant, set)
}
