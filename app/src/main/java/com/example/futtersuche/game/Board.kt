package com.example.futtersuche.game

import kotlin.math.abs
import kotlin.random.Random

/** Spezialfähigkeit eines Steins. */
enum class Special {
    NONE,

    /** Aus einer waagerechten 4er-Reihe: löscht beim Auflösen die ganze Zeile. */
    LINE_H,

    /** Aus einer senkrechten 4er-Reihe: löscht beim Auflösen die ganze Spalte. */
    LINE_V,

    /**
     * Aus einer 5er-Reihe: Tauscht man ihn mit einem Nachbarn, verschwinden alle Steine
     * dieser Nachbar-Sorte. Wird er anders aufgelöst, verschwinden alle Steine seiner eigenen Sorte.
     */
    COLOR_BOMB,
}

/**
 * Ein Stein auf dem Spielfeld.
 * [kind]    = Nummer der Futtersorte (0 bis Anzahl Sorten - 1).
 * [id]      = eindeutige Nummer, damit wir den Stein später beim Animieren wiedererkennen.
 * [special] = Spezialfähigkeit (normaler Stein: NONE).
 */
data class Tile(val id: Long, val kind: Int, val special: Special = Special.NONE) {
    val isSpecial: Boolean get() = special != Special.NONE

    companion object {
        /** Erzeugt einen neuen, normalen Stein mit zufälliger Sorte. */
        fun random(kindCount: Int, random: Random): Tile = Tile(TileIds.next(), random.nextInt(kindCount))
    }
}

/** Eine Position auf dem Spielfeld (Zeile, Spalte – beides ab 0 gezählt). */
data class Pos(val row: Int, val col: Int) {
    /** true, wenn [other] direkt links, rechts, oben oder unten daneben liegt. */
    fun isNeighborOf(other: Pos): Boolean = abs(row - other.row) + abs(col - other.col) == 1
}

/**
 * Das Spielfeld. Es ist unveränderlich: Jede Änderung erzeugt ein neues Board.
 * Das ist für Jetpack Compose praktisch, weil es so Änderungen sicher bemerkt.
 * Ein Feld kann leer sein (null), z. B. nachdem Steine entfernt wurden.
 */
class Board(val rows: Int, val cols: Int, private val cells: List<Tile?>) {

    init {
        require(cells.size == rows * cols) { "Falsche Anzahl an Feldern" }
    }

    operator fun get(row: Int, col: Int): Tile? = cells[row * cols + col]

    operator fun get(pos: Pos): Tile? = this[pos.row, pos.col]

    fun isInside(row: Int, col: Int): Boolean = row in 0 until rows && col in 0 until cols

    fun isInside(pos: Pos): Boolean = isInside(pos.row, pos.col)

    /** Liefert true, wenn irgendwo 3 (oder mehr) gleiche Steine in einer Reihe/Spalte liegen. */
    fun hasAnyMatch(): Boolean = MatchFinder.find(this).isNotEmpty()

    /** Alle Positionen des Feldes, zeilenweise von links oben nach rechts unten. */
    fun positions(): List<Pos> = (0 until rows).flatMap { row -> (0 until cols).map { col -> Pos(row, col) } }

    /** Neues Board, bei dem die Felder aus [changes] ersetzt sind (null = leer). */
    fun withTiles(changes: Map<Pos, Tile?>): Board {
        val newCells = cells.toMutableList()
        for ((pos, tile) in changes) newCells[index(pos)] = tile
        return Board(rows, cols, newCells)
    }

    /** Neues Board, bei dem die Steine an [a] und [b] vertauscht sind. */
    fun swap(a: Pos, b: Pos): Board = withTiles(mapOf(a to this[b], b to this[a]))

    /** Neues Board, bei dem alle Steine an den [positions] entfernt (leer) sind. */
    fun removeAll(positions: Collection<Pos>): Board = withTiles(positions.associateWith { null })

    private fun index(pos: Pos): Int = pos.row * cols + pos.col
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
     * Außerdem muss es mindestens einen möglichen Zug geben, sonst würfeln wir neu.
     */
    fun create(
        rows: Int = 8,
        cols: Int = 8,
        kindCount: Int = 5,
        random: Random = Random.Default,
    ): Board {
        require(kindCount >= 3) { "Es werden mindestens 3 Steinarten benötigt" }
        while (true) {
            val board = fillWithoutMatches(rows, cols, kindCount, random)
            if (MoveFinder.hasPossibleMove(board)) return board
        }
    }

    private fun fillWithoutMatches(rows: Int, cols: Int, kindCount: Int, random: Random): Board {
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
