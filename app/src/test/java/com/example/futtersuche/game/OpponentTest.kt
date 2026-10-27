package com.example.futtersuche.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class OpponentTest {

    private val random = Random(5)

    private fun stateOf(
        board: Board,
        opponent: OpponentConfig,
        goals: List<Goal> = listOf(Goal(0, 50)),
        movesMade: Int = 1,
        collected: Map<Int, Int> = emptyMap(),
        thiefTargetId: Long? = null,
    ): LevelState {
        val config = LevelConfig(20, moves = 20, kindCount = 5, goals = goals, opponent = opponent)
        return LevelState(config, board, movesLeft = 20 - movesMade, collected = collected, thiefTargetId = thiefTargetId)
    }

    @Test
    fun trefferNebenHindernisEntferntEs() {
        val board = boardOf(
            "1232",
            "0004",
            "2343",
        ).withBlockers(obstacles = setOf(Pos(2, 1)))
        val step = Cascade.resolve(board, 5, random).first()
        assertEquals(setOf(Pos(2, 1)), step.obstaclesRemoved)
        assertFalse(Pos(2, 1) in step.afterFall.obstacles)
    }

    @Test
    fun hindernisWeitWegBleibtLiegen() {
        val board = boardOf(
            "0001",
            "1234",
            "2341",
        ).withBlockers(obstacles = setOf(Pos(2, 3)))
        val step = Cascade.resolve(board, 5, random).first()
        assertTrue(step.obstaclesRemoved.isEmpty())
        assertTrue(Pos(2, 3) in step.afterFall.obstacles)
    }

    @Test
    fun hindernisseUnterbrechenReihenUndSindNichtTauschbar() {
        val board = boardOf("00.0", "1212").withBlockers(obstacles = setOf(Pos(0, 2)))
        assertTrue(MatchFinder.find(board).isEmpty())
        assertEquals(SwapResult.NotAllowed, GameRules.trySwap(board, Pos(0, 1), Pos(0, 2), 5, random))
    }

    @Test
    fun steineRutschenAnHindernissenVorbei() {
        val board = boardOf(
            "1",
            ".",
            ".",
            ".",
        ).withBlockers(obstacles = setOf(Pos(2, 0)))
        val fallen = Gravity.apply(board, 5, random)
        assertEquals(1, fallen[3, 0]?.kind) // an dem Hindernis vorbei bis ganz unten
        assertNull(fallen[2, 0])             // das Hindernis-Feld bleibt leer
        assertNotNull(fallen[0, 0])
        assertNotNull(fallen[1, 0])
    }

    @Test
    fun trefferNebenEindringlingVertreibtIhn() {
        val board = boardOf(
            "1232",
            "0004",
            "2343",
        ).withBlockers(intruder = Intruder(Pos(0, 0), patience = 3))
        val step = Cascade.resolve(board, 5, random).first()
        assertTrue(step.intruderChased)
        assertNull(step.afterFall.intruder)
    }

    @Test
    fun diebKlautMarkiertesFutterNachDemNaechstenZug() {
        val board = BoardGenerator.create(random = Random(1))
        val target = board[Pos(4, 4)]!!
        val state = stateOf(board, OpponentConfig(thiefInterval = 3), thiefTargetId = target.id)

        val turn = OpponentAI.act(state, random)
        assertTrue(turn.events.any { it is OpponentEvent.ThiefStole })
        assertNull(turn.state.thiefTargetId)
        assertTrue(turn.state.board.positions().none { turn.state.board[it]?.id == target.id })
    }

    @Test
    fun eingesammeltesFutterIstGerettet() {
        val board = BoardGenerator.create(random = Random(1))
        val state = stateOf(board, OpponentConfig(thiefInterval = 3), thiefTargetId = -42L) // nicht mehr auf dem Feld
        val turn = OpponentAI.act(state, random)
        assertTrue(OpponentEvent.ThiefFoiled in turn.events)
    }

    @Test
    fun diebMarkiertBevorzugtGewuenschtesFutter() {
        repeat(20) { seed ->
            val board = BoardGenerator.create(random = Random(seed))
            val state = stateOf(board, OpponentConfig(thiefInterval = 3), goals = listOf(Goal(2, 10)), movesMade = 3)
            val turn = OpponentAI.act(state, Random(seed))
            val id = turn.state.thiefTargetId!!
            val kind = turn.afterActions.positions().firstNotNullOf { p -> turn.afterActions[p]?.takeIf { it.id == id } }.kind
            assertEquals(2, kind)
        }
    }

    @Test
    fun eindringlingRichtetNachDreiZuegenSchadenAn() {
        var board = BoardGenerator.create(random = Random(2))
        var state = stateOf(board, OpponentConfig(intruderInterval = 1), collected = mapOf(0 to 10), movesMade = 4)

        state = OpponentAI.act(state, random).state
        assertEquals(Difficulty.INTRUDER_PATIENCE, state.board.intruder?.patience)

        repeat(Difficulty.INTRUDER_PATIENCE - 1) { state = OpponentAI.act(state, random).state }
        assertEquals(1, state.board.intruder?.patience)

        val turn = OpponentAI.act(state, random)
        assertNull(turn.state.board.intruder) // er ist wieder weg …
        val damage = turn.events.filter { it is OpponentEvent.IntruderStole || it is OpponentEvent.IntruderBlocked }
        assertEquals(1, damage.size)          // … hat aber Schaden angerichtet
        if (damage[0] is OpponentEvent.IntruderStole) {
            assertTrue((turn.state.collected[0] ?: 0) < 10 + turn.steps.sumOf { it.collected[0] ?: 0 })
        }
        board = turn.state.board
        assertTrue(MoveFinder.hasPossibleMove(board))
    }

    @Test
    fun ohneWidersacherPassiertNichts() {
        val board = BoardGenerator.create(random = Random(3))
        val state = stateOf(board, OpponentConfig())
        val turn = OpponentAI.act(state, random)
        assertTrue(turn.events.isEmpty())
        assertEquals(state.board.kindsAsText(), turn.state.board.kindsAsText())
        assertEquals(state.collected, turn.state.collected)
        assertEquals(state.score, turn.state.score)
    }

    @Test
    fun levelGenerierungSchaltetWidersacherNachUndNachFrei() {
        assertFalse(LevelGenerator.generate(4, "x").opponent.blockade)
        assertTrue(LevelGenerator.generate(5, "x").opponent.blockade)
        assertFalse(LevelGenerator.generate(11, "x").opponent.thief)
        assertTrue(LevelGenerator.generate(12, "x").opponent.thief)
        assertFalse(LevelGenerator.generate(19, "x").opponent.intruder)
        assertTrue(LevelGenerator.generate(20, "x").opponent.intruder)
        val start = LevelState.start(LevelGenerator.generate(5, "x"), random)
        assertEquals(Difficulty.START_OBSTACLES, start.board.obstacles.size)
    }
}
