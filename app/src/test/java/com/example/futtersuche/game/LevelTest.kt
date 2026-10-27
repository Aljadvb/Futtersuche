package com.example.futtersuche.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LevelTest {

    @Test
    fun schwierigkeitFolgtDenVorgaben() {
        assertEquals(25, Difficulty.moves(1))
        assertEquals(15, Difficulty.moves(100))
        assertEquals(1, Difficulty.goalCount(5))
        assertEquals(2, Difficulty.goalCount(6))
        assertEquals(3, Difficulty.goalCount(15))
        assertEquals(5, Difficulty.kindCount(19))
        assertEquals(6, Difficulty.kindCount(20))
        assertEquals(10, Difficulty.amountPerGoal(1))
    }

    @Test
    fun werteAendernSichNurInKleinenSchritten() {
        for (level in 1..99) {
            val diff = Difficulty.moves(level) - Difficulty.moves(level + 1)
            assertTrue("Züge springen bei Level $level", diff in 0..1)
        }
        for (level in 1..99) {
            if (Difficulty.goalCount(level) != Difficulty.goalCount(level + 1)) continue
            val diff = Difficulty.amountPerGoal(level + 1) - Difficulty.amountPerGoal(level)
            assertTrue("Menge springt bei Level $level", diff in -2..1)
        }
    }

    @Test
    fun gleichesLevelHatImmerDieselbenZiele() {
        for (level in 1..30) {
            assertEquals(LevelGenerator.generate(level, "garten"), LevelGenerator.generate(level, "garten"))
        }
        val goals = LevelGenerator.generate(20, "garten").goals
        assertEquals(goals.size, goals.map { it.kind }.toSet().size) // keine Sorte doppelt
    }

    @Test
    fun punkteFuerTrefferUndKetten() {
        assertEquals(30, Scoring.pointsForMatch(3))
        assertEquals(60, Scoring.pointsForMatch(4))
        assertEquals(100, Scoring.pointsForMatch(5))
        assertEquals(1.0, Scoring.chainMultiplier(1), 0.0)
        assertTrue(Scoring.chainMultiplier(3) > Scoring.chainMultiplier(2))
    }

    @Test
    fun zugZaehltFortschrittUndPunkte() {
        val config = LevelConfig(1, moves = 2, kindCount = 5, goals = listOf(Goal(0, 3)))
        val board = boardOf(
            "0010",
            "1201",
            "2312",
        )
        val start = LevelState(config, board, movesLeft = 2)
        val result = GameRules.trySwap(board, Pos(0, 2), Pos(0, 3), 5, Random(1)) as SwapResult.Success
        val after = start.afterMove(result)

        assertEquals(1, after.movesLeft)
        assertEquals(3, after.progress(config.goals[0]))
        assertTrue(after.score >= 30)
        assertEquals(LevelStatus.WON, after.status)
        assertEquals(after.score + 50, after.finalScore) // 1 übriger Zug = 50 Bonus
    }

    @Test
    fun ohneZuegeUndOhneZielIstVerloren() {
        val config = LevelConfig(1, moves = 1, kindCount = 5, goals = listOf(Goal(0, 99)))
        val state = LevelState(config, BoardGenerator.create(), movesLeft = 0)
        assertEquals(LevelStatus.LOST, state.status)
        assertEquals(0, state.leftoverBonus)
    }
}
