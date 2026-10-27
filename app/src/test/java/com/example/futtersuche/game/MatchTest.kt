package com.example.futtersuche.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchTest {

    /** Baut ein Board aus Text: jede Ziffer ist eine Sorte, "." ist ein leeres Feld. */
    private fun boardOf(vararg lines: String): Board {
        val cells = lines.flatMap { line ->
            line.map { c -> if (c == '.') null else Tile(TileIds.next(), c.digitToInt()) }
        }
        return Board(lines.size, lines[0].length, cells)
    }

    @Test
    fun findetWaagerechteUndSenkrechteReihen() {
        val board = boardOf(
            "00012",
            "34123",
            "34412",
            "31234",
            "32041",
        )
        val matches = MatchFinder.find(board)
        assertEquals(2, matches.size)
        assertTrue(matches.any { it.horizontal && it.kind == 0 && it.size == 3 })
        assertTrue(matches.any { !it.horizontal && it.kind == 3 && it.size == 4 })
    }

    @Test
    fun leereFelderZaehlenNicht() {
        val board = boardOf("..012", "01201")
        assertTrue(MatchFinder.find(board).isEmpty())
    }

    @Test
    fun tauschOhneTrefferWirdAbgelehnt() {
        val board = boardOf(
            "012",
            "120",
            "201",
        )
        assertTrue(GameRules.trySwap(board, Pos(0, 0), Pos(0, 1)) is SwapResult.NoMatch)
    }

    @Test
    fun nichtBenachbarteFelderSindNichtErlaubt() {
        val board = boardOf("0120", "1201")
        assertEquals(SwapResult.NotAllowed, GameRules.trySwap(board, Pos(0, 0), Pos(1, 1)))
        assertEquals(SwapResult.NotAllowed, GameRules.trySwap(board, Pos(0, 0), Pos(0, 2)))
    }

    @Test
    fun tauschMitTrefferEntferntDieSteine() {
        val board = boardOf(
            "0010",
            "1201",
        )
        // Rechte 0 oben mit der 1 links daneben tauschen → oben "0001"
        val result = GameRules.trySwap(board, Pos(0, 2), Pos(0, 3))
        assertTrue(result is SwapResult.Success)
        result as SwapResult.Success
        assertEquals(setOf(Pos(0, 0), Pos(0, 1), Pos(0, 2)), result.removed)
        assertEquals(null, result.cleared[0, 0])
        assertEquals(1, result.cleared[0, 3]?.kind)
    }

    @Test
    fun lFormZaehltDenEckSteinNurEinmal() {
        val board = boardOf(
            "000",
            "011",
            "022",
        )
        val removedByMatches = MatchFinder.find(board).flatMap { it.cells }.toSet()
        assertEquals(5, removedByMatches.size)
    }
}
