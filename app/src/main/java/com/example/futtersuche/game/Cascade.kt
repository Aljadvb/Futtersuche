package com.example.futtersuche.game

import kotlin.random.Random

/**
 * Ein Durchgang beim Auflösen: Treffer verschwinden, Spezialsteine entstehen,
 * alles rutscht nach und neue Steine fallen von oben nach.
 * Die UI spielt diese Schritte nacheinander ab.
 */
data class CascadeStep(
    /** 1 = direkter Treffer durch den Zug, 2 = erste Kettenreaktion, 3 = zweite … */
    val chain: Int,
    /** Feld vor diesem Durchgang. */
    val before: Board,
    /** Gefundene Reihen (für Punkte in Schritt 4). */
    val matches: List<Match>,
    /** Alle Felder, die in diesem Durchgang leer werden (Reihen + Spezialstein-Effekte). */
    val removed: Set<Pos>,
    /** Neu entstandene Spezialsteine. */
    val created: Map<Pos, Tile>,
    /** Gesammeltes Futter: Sorte → Anzahl. */
    val collected: Map<Int, Int>,
    /** Feld nach dem Entfernen (mit Lücken). */
    val afterRemoval: Board,
    /** Feld nach dem Nachrutschen und Auffüllen. */
    val afterFall: Board,
)

object Cascade {

    /** Sicherheitsgrenze, damit eine Endlos-Kette nie das Spiel einfriert. */
    private const val MAX_CHAIN = 50

    /**
     * Löst das Feld so lange auf, bis keine Treffer mehr da sind.
     *
     * [forcedRemoval] = Felder, die im ersten Durchgang zusätzlich verschwinden müssen
     *                   (z. B. alle Steine einer Sorte nach einem Farbbomben-Tausch).
     * [preferredSpecialSpots] = Hier sollen neue Spezialsteine bevorzugt entstehen
     *                   (die beiden getauschten Felder – dort hat der Spieler hingeschaut).
     */
    fun resolve(
        start: Board,
        kindCount: Int,
        random: Random,
        forcedRemoval: Set<Pos> = emptySet(),
        preferredSpecialSpots: Set<Pos> = emptySet(),
    ): List<CascadeStep> {
        val steps = mutableListOf<CascadeStep>()
        var current = start
        var forced = forcedRemoval
        var preferred = preferredSpecialSpots

        for (chain in 1..MAX_CHAIN) {
            val matches = MatchFinder.find(current)
            if (matches.isEmpty() && forced.isEmpty()) break

            val created = planSpecials(current, matches, preferred)
            val hit = matches.flatMap { it.cells }.toSet() + forced
            val removed = SpecialEffects.expand(current, hit) - created.keys

            val collected = removed.mapNotNull { current[it]?.kind }.groupingBy { it }.eachCount()
            val afterRemoval = current.removeAll(removed).withTiles(created)
            val afterFall = Gravity.apply(afterRemoval, kindCount, random)

            steps += CascadeStep(chain, current, matches, removed, created, collected, afterRemoval, afterFall)
            current = afterFall
            forced = emptySet()
            preferred = emptySet()
        }
        return steps
    }

    /**
     * Legt fest, wo aus 4er- und 5er-Reihen Spezialsteine entstehen.
     * Der Spezialstein übernimmt die Sorte der Reihe und bleibt liegen, statt zu verschwinden.
     */
    private fun planSpecials(board: Board, matches: List<Match>, preferred: Set<Pos>): Map<Pos, Tile> {
        val created = mutableMapOf<Pos, Tile>()
        for (match in matches.filter { it.size >= 4 }.sortedByDescending { it.size }) {
            val free = match.cells.filter { it !in created }
            val spot = free.firstOrNull { it in preferred } ?: free.getOrNull(free.size / 2) ?: continue
            val special = when {
                match.size >= 5 -> Special.COLOR_BOMB
                match.horizontal -> Special.LINE_H
                else -> Special.LINE_V
            }
            val old = board[spot] ?: continue
            created[spot] = old.copy(kind = match.kind, special = special)
        }
        return created
    }
}

object SpecialEffects {

    /**
     * Nimmt die Felder [hit] und ergänzt alle Felder, die durch darin enthaltene
     * Spezialsteine zusätzlich gelöscht werden. Trifft ein Effekt einen weiteren
     * Spezialstein, löst dieser ebenfalls aus (Kettenreaktion).
     */
    fun expand(board: Board, hit: Set<Pos>): Set<Pos> {
        val result = hit.filter { board[it] != null }.toMutableSet()
        val queue = ArrayDeque(result.filter { board[it]!!.isSpecial })

        while (queue.isNotEmpty()) {
            val pos = queue.removeFirst()
            val tile = board[pos] ?: continue
            for (target in affectedBy(board, pos, tile)) {
                val targetTile = board[target] ?: continue
                if (result.add(target) && targetTile.isSpecial) queue += target
            }
        }
        return result
    }

    /** Welche Felder löscht dieser Spezialstein? */
    private fun affectedBy(board: Board, pos: Pos, tile: Tile): List<Pos> = when (tile.special) {
        Special.NONE -> emptyList()
        Special.LINE_H -> (0 until board.cols).map { Pos(pos.row, it) }
        Special.LINE_V -> (0 until board.rows).map { Pos(it, pos.col) }
        Special.COLOR_BOMB -> allOfKind(board, tile.kind)
    }

    fun allOfKind(board: Board, kind: Int): List<Pos> = board.positions().filter { board[it]?.kind == kind }
}

object Gravity {

    /**
     * Lässt in jeder Spalte die Steine nach unten in die Lücken rutschen
     * und füllt oben mit neuen, zufälligen Steinen auf.
     */
    fun apply(board: Board, kindCount: Int, random: Random): Board {
        val changes = mutableMapOf<Pos, Tile?>()
        for (col in 0 until board.cols) {
            // Steine dieser Spalte von unten nach oben einsammeln
            val tiles = (board.rows - 1 downTo 0).mapNotNull { row -> board[row, col] }
            // … und von unten her wieder hineinlegen, oben neue Steine ergänzen
            for ((i, row) in (board.rows - 1 downTo 0).withIndex()) {
                changes[Pos(row, col)] = tiles.getOrNull(i) ?: Tile.random(kindCount, random)
            }
        }
        return board.withTiles(changes)
    }
}
