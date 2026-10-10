package com.bashkevich.tennisscorekeeperbackend.model.match.doubles

// запись очереди подающих матча для конкретного сета:
// какой игрок пары подает первым и какая по счету пара подает в сете (1 или 2)
data class DoublesServeRecord(
    val participantId: Int,
    val serveOrder: Int,
    val playerId: Int,
    val pointNumberRedoLimit: Int? = null,
)
