package com.example.futtersuche.game

import kotlin.random.Random

/**
 * Ein Stein auf dem Spielfeld.
 * [kind] = Nummer der Futtersorte (0 bis Anzahl Sorten - 1).
 * [id]   = eindeutige Nummer, damit wir den Stein später beim Animieren wiedererkennen.
 */
data class Tile(val id: Long, val kind: Int)

/**
 * Das Spielfeld. Es ist unveränderlich: Jede Änderung erzeugt ein neues Board.
 * Das ist für Jetpack Compose praktisch, weil es so Änderungen sicher bemerkt.
 * Ein Feld kann leer sein (null) – das brauchen wir ab Schritt 2/3.
 */
class Board(val rows: Int, val cols: Int, private val cells: List<Tile?>) {

    init {
        require(cells.size == rows * cols) { "Falsche Anzahl an Feldern" }
    }

    operator fun get(row: Int, col: Int): Tile? = cells[row * cols + col]

    fun isInside(row: Int, col: Int): Boolean = row in 0 until rows && col in 0 until cols

    /** Liefert true, wenn irgendwo 3 (oder mehr) gleiche Steine in einer Reihe/Spalte liegen. */
    fun hasAnyMatch(): Boolean {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val kind = this[row, col]?.kind ?: continue
                val horizontal = col + 2 < cols &&
                    this[row, col + 1]?.kind == kind && this[row, col + 2]?.kind == kind
                val vertical = row + 2 < rows &&
                    this[row + 1, col]?.kind == kind && this[row + 2, col]?.kind == kind
                if (horizontal || vertical) return true
            }
        }
        return false
    }
}

/** Erzeugt fortlaufende, eindeutige Stein-Nummern. */
object TileIds {
    private var next = 0L
    fun next(): Long = next++
}

object BoardGenerator {

    /**
     * Füllt ein neues Spielfeld zufällig, ohne dass dabei schon 3er-Reihen entstehen.
     *
     * Idee: Wir füllen Feld für Feld von links oben nach rechts unten.
     * Für jedes Feld schauen wir nur nach links und nach oben:
     *  - Sind die zwei Steine links gleich, darf diese Sorte hier nicht hin.
     *  - Sind die zwei Steine darüber gleich, darf diese Sorte hier auch nicht hin.
     * Es fallen also höchstens 2 Sorten weg – bei 5 Sorten bleiben immer mindestens 3 übrig.
     */
    fun create(
        rows: Int = 8,
        cols: Int = 8,
        kindCount: Int = 5,
        random: Random = Random.Default,
    ): Board {
        require(kindCount >= 3) { "Es werden mindestens 3 Steinarten benötigt" }

        val kinds = IntArray(rows * cols)
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val forbidden = mutableSetOf<Int>()
                if (col >= 2) {
                    val left1 = kinds[row * cols + col - 1]
                    val left2 = kinds[row * cols + col - 2]
                    if (left1 == left2) forbidden += left1
                }
                if (row >= 2) {
                    val up1 = kinds[(row - 1) * cols + col]
                    val up2 = kinds[(row - 2) * cols + col]
                    if (up1 == up2) forbidden += up1
                }
                val allowed = (0 until kindCount).filter { it !in forbidden }
                kinds[row * cols + col] = allowed.random(random)
            }
        }

        val board = Board(rows, cols, kinds.map { Tile(TileIds.next(), it) })
        check(!board.hasAnyMatch()) { "Generator-Fehler: Feld enthält eine 3er-Reihe" }
        return board
    }
}
