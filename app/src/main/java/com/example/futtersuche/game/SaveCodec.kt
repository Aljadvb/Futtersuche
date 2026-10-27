package com.example.futtersuche.game

/**
 * Wandelt einen laufenden Level-Stand in Text um (und zurück), damit er gespeichert werden kann.
 *
 * Das Format ist bewusst einfach und lesbar, eine Zeile pro Wert, z. B.:
 *   level=12
 *   movesLeft=14
 *   tiles=0,3h,.,2b,…   (Sorte + Spezial-Kürzel, "." = leer/blockiert)
 *   obstacles=3:4,5:1   (Zeile:Spalte)
 *
 * Die Level-Einstellungen (Ziele, Widersacher …) werden nicht gespeichert, sondern
 * aus der Levelnummer neu berechnet – sie sind ja immer gleich.
 */
object SaveCodec {

    private const val VERSION = "1"

    fun encode(state: LevelState): String {
        val board = state.board
        // Steine bekommen beim Laden neue IDs – die Dieb-Markierung merken wir uns deshalb als Position.
        val thiefPos = state.thiefTargetId?.let { id -> board.positions().firstOrNull { board[it]?.id == id } }
        return listOf(
            "version=$VERSION",
            "level=${state.config.number}",
            "movesLeft=${state.movesLeft}",
            "score=${state.score}",
            "collected=" + state.collected.entries.joinToString(",") { "${it.key}:${it.value}" },
            "size=${board.rows}x${board.cols}",
            "tiles=" + board.positions().joinToString(",") { pos ->
                board[pos]?.let { "${it.kind}${specialCode(it.special)}" } ?: "."
            },
            "obstacles=" + board.obstacles.joinToString(",") { "${it.row}:${it.col}" },
            "intruder=" + (board.intruder?.let { "${it.pos.row}:${it.pos.col}:${it.patience}" } ?: ""),
            "thief=" + (thiefPos?.let { "${it.row}:${it.col}" } ?: ""),
            "obstacleDue=${state.obstacleDue}",
        ).joinToString("\n")
    }

    /**
     * Liest einen gespeicherten Stand. Gibt null zurück, wenn der Text kaputt ist oder
     * nicht zu den Level-Einstellungen passt – dann startet einfach ein neues Level.
     */
    fun decode(text: String, campaignId: String): LevelState? = try {
        val values = text.lines().filter { "=" in it }.associate { it.substringBefore("=") to it.substringAfter("=") }
        require(values["version"] == VERSION)

        val config = LevelGenerator.generate(values.getValue("level").toInt(), campaignId)
        val (rows, cols) = values.getValue("size").split("x").map { it.toInt() }
        val tiles = values.getValue("tiles").split(",").map { code ->
            if (code == ".") null else {
                val kind = code.takeWhile { it.isDigit() }.toInt()
                require(kind in 0 until config.kindCount)
                Tile(TileIds.next(), kind, specialFromCode(code.drop(kind.toString().length)))
            }
        }
        val obstacles = parsePositions(values["obstacles"]).toSet()
        val intruder = values["intruder"].orEmpty().takeIf { it.isNotEmpty() }?.let {
            val (row, col, patience) = it.split(":").map { n -> n.toInt() }
            Intruder(Pos(row, col), patience)
        }
        val board = Board(rows, cols, tiles).withBlockers(obstacles, intruder)
        val thiefId = parsePositions(values["thief"]).firstOrNull()?.let { board[it]?.id }
        val collected = values["collected"].orEmpty().split(",").filter { it.isNotEmpty() }.associate {
            val (kind, count) = it.split(":").map { n -> n.toInt() }
            kind to count
        }

        LevelState(
            config = config,
            board = board,
            movesLeft = values.getValue("movesLeft").toInt(),
            collected = collected,
            score = values.getValue("score").toInt(),
            thiefTargetId = thiefId,
            obstacleDue = values["obstacleDue"] == "true",
        )
    } catch (e: Exception) {
        null
    }

    private fun parsePositions(text: String?): List<Pos> =
        text.orEmpty().split(",").filter { it.isNotEmpty() }.map {
            val (row, col) = it.split(":").map { n -> n.toInt() }
            Pos(row, col)
        }

    private fun specialCode(special: Special): String = when (special) {
        Special.NONE -> ""
        Special.LINE_H -> "h"
        Special.LINE_V -> "v"
        Special.COLOR_BOMB -> "b"
    }

    private fun specialFromCode(code: String): Special = when (code) {
        "" -> Special.NONE
        "h" -> Special.LINE_H
        "v" -> Special.LINE_V
        "b" -> Special.COLOR_BOMB
        else -> throw IllegalArgumentException("Unbekannter Spezialstein: $code")
    }
}
