package com.example.futtersuche.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.random.Random

class BoardGeneratorTest {

    @Test
    fun neuesFeldHatKeineDreierReihen() {
        repeat(1000) { seed ->
            val board = BoardGenerator.create(kindCount = 5, random = Random(seed))
            assertFalse("Seed $seed enthält eine 3er-Reihe", board.hasAnyMatch())
        }
    }

    @Test
    fun feldIstVollUndNutztNurErlaubteSorten() {
        val board = BoardGenerator.create(kindCount = 6, random = Random(42))
        for (row in 0 until 8) for (col in 0 until 8) {
            val tile = board[row, col]!!
            assert(tile.kind in 0 until 6)
        }
        assertEquals(8, board.rows)
    }
}
