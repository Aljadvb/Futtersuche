package com.example.futtersuche.game

/**
 * Baut ein Board aus Text: jede Ziffer ist eine Sorte, "." ist ein leeres Feld.
 * Spezialsteine: "h" + Ziffer = LINE_H, "v" + Ziffer = LINE_V, "b" + Ziffer = COLOR_BOMB.
 */
fun boardOf(vararg lines: String): Board {
    val rows = lines.map { line ->
        val cells = mutableListOf<Tile?>()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            val special = when (c) {
                'h' -> Special.LINE_H
                'v' -> Special.LINE_V
                'b' -> Special.COLOR_BOMB
                else -> null
            }
            if (special != null) {
                cells += Tile(TileIds.next(), line[i + 1].digitToInt(), special)
                i += 2
            } else {
                cells += if (c == '.') null else Tile(TileIds.next(), c.digitToInt())
                i += 1
            }
        }
        cells
    }
    return Board(rows.size, rows[0].size, rows.flatten())
}

/** Schreibt die Sorten eines Boards als Text (zum Vergleichen in Tests). */
fun Board.kindsAsText(): List<String> = (0 until rows).map { row ->
    (0 until cols).joinToString("") { col -> this[row, col]?.kind?.toString() ?: "." }
}
