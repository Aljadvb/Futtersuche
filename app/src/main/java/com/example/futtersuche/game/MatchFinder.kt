package com.example.futtersuche.game

/**
 * Ein Treffer: mindestens 3 gleiche Steine direkt hintereinander in einer Zeile oder Spalte.
 * [cells] enthält alle Positionen der Reihe (3, 4, 5 oder mehr).
 * Die Länge brauchen wir in Schritt 3 für Spezialsteine und in Schritt 4 für Punkte.
 */
data class Match(val kind: Int, val cells: List<Pos>, val horizontal: Boolean) {
    val size: Int get() = cells.size
}

object MatchFinder {

    /** Sucht alle Treffer auf dem Feld (waagerecht und senkrecht). */
    fun find(board: Board): List<Match> {
        val matches = mutableListOf<Match>()

        // Waagerecht: jede Zeile von links nach rechts durchgehen
        for (row in 0 until board.rows) {
            scanLine(board.cols, { col -> board[row, col]?.kind }, { col -> Pos(row, col) }, true, matches)
        }
        // Senkrecht: jede Spalte von oben nach unten durchgehen
        for (col in 0 until board.cols) {
            scanLine(board.rows, { row -> board[row, col]?.kind }, { row -> Pos(row, col) }, false, matches)
        }
        return matches
    }

    /**
     * Läuft eine Linie (Zeile oder Spalte) ab und merkt sich, wie viele gleiche Steine
     * hintereinander liegen. Endet eine Folge mit Länge >= 3, ist das ein Treffer.
     */
    private fun scanLine(
        length: Int,
        kindAt: (Int) -> Int?,
        posAt: (Int) -> Pos,
        horizontal: Boolean,
        out: MutableList<Match>,
    ) {
        var runStart = 0
        for (i in 1..length) {
            val runKind = kindAt(runStart)
            val continues = i < length && runKind != null && kindAt(i) == runKind
            if (!continues) {
                val runLength = i - runStart
                if (runKind != null && runLength >= 3) {
                    out += Match(runKind, (runStart until i).map(posAt), horizontal)
                }
                runStart = i
            }
        }
    }
}
