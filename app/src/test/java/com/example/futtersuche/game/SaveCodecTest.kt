package com.example.futtersuche.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class SaveCodecTest {

    @Test
    fun spielstandWirdVollstaendigWiederhergestellt() {
        val config = LevelGenerator.generate(22, "garten")
        val start = LevelState.start(config, Random(4))
        var board = start.board
            .withTiles(mapOf(Pos(0, 0) to Tile(TileIds.next(), 1, Special.LINE_H), Pos(0, 1) to Tile(TileIds.next(), 2, Special.COLOR_BOMB)))
            .withBlockers(obstacles = setOf(Pos(5, 5), Pos(6, 2)), intruder = Intruder(Pos(3, 3), patience = 2))
        val thiefTarget = board[Pos(7, 7)]!!
        val state = start.copy(
            board = board,
            movesLeft = 9,
            collected = mapOf(0 to 4, 3 to 7),
            score = 1230,
            thiefTargetId = thiefTarget.id,
        )

        val loaded = SaveCodec.decode(SaveCodec.encode(state), "garten")
        assertNotNull(loaded)
        loaded!!
        board = loaded.board

        assertEquals(state.config, loaded.config)
        assertEquals(9, loaded.movesLeft)
        assertEquals(mapOf(0 to 4, 3 to 7), loaded.collected)
        assertEquals(1230, loaded.score)
        assertEquals(state.board.kindsAsText(), board.kindsAsText())
        assertEquals(Special.LINE_H, board[0, 0]?.special)
        assertEquals(Special.COLOR_BOMB, board[0, 1]?.special)
        assertEquals(setOf(Pos(5, 5), Pos(6, 2)), board.obstacles)
        assertEquals(Intruder(Pos(3, 3), 2), board.intruder)
        assertEquals(board[7, 7]?.id, loaded.thiefTargetId) // Markierung sitzt wieder auf demselben Feld
    }

    @Test
    fun ohneDiebUndEindringlingKlapptEsAuch() {
        val state = LevelState.start(LevelGenerator.generate(1, "garten"), Random(2))
        val loaded = SaveCodec.decode(SaveCodec.encode(state), "garten")!!
        assertNull(loaded.thiefTargetId)
        assertNull(loaded.board.intruder)
        assertEquals(state.board.kindsAsText(), loaded.board.kindsAsText())
    }

    @Test
    fun kaputterTextErgibtNull() {
        assertNull(SaveCodec.decode("", "garten"))
        assertNull(SaveCodec.decode("version=1\nlevel=abc", "garten"))
        assertNull(SaveCodec.decode("version=99\nlevel=1", "garten"))
    }
}
