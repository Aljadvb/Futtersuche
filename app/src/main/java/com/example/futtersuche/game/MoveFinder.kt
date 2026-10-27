package com.example.futtersuche.game

import kotlin.random.Random

object MoveFinder {

    /** Sucht einen Tausch, der einen Treffer ergibt (oder eine Farbbombe auslöst). null = keiner da. */
    fun findMove(board: Board): Pair<Pos, Pos>? {
        for (pos in board.positions()) {
            for (neighbor in listOf(Pos(pos.row, pos.col + 1), Pos(pos.row + 1, pos.col))) {
                if (!board.isInside(neighbor)) continue
                val a = board[pos] ?: continue
                val b = board[neighbor] ?: continue
                if (a.special == Special.COLOR_BOMB || b.special == Special.COLOR_BOMB) return pos to neighbor
                if (MatchFinder.find(board.swap(pos, neighbor)).isNotEmpty()) return pos to neighbor
            }
        }
        return null
    }

    fun hasPossibleMove(board: Board): Boolean = findMove(board) != null

    /**
     * Mischt die vorhandenen Steine neu, bis kein Treffer auf dem Feld liegt
     * und mindestens ein Zug möglich ist. Spezialsteine bleiben erhalten.
     * Klappt das nach vielen Versuchen nicht, gibt es ein komplett neues Feld.
     */
    fun shuffle(board: Board, kindCount: Int, random: Random): Board {
        val filled = board.positions().filter { board[it] != null }
        val tiles = filled.map { board[it]!! }
        repeat(200) {
            val shuffled = board.withTiles(filled.zip(tiles.shuffled(random)).toMap())
            if (!shuffled.hasAnyMatch() && hasPossibleMove(shuffled)) return shuffled
        }
        return BoardGenerator.create(board.rows, board.cols, kindCount, random)
    }
}
