package com.example.futtersuche.game

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Prüft, ob die Level schaffbar sind: Ein einfacher Computer-Spieler spielt jedes Level
 * mehrmals durch – inklusive Widersacher. Er wählt immer den Zug, der am meisten noch
 * benötigtes Futter bringt, kümmert sich etwas um den Widersacher, plant aber nicht
 * voraus – er spielt also eher wie ein Anfänger.
 *
 * Nach Änderungen in Difficulty.kt diesen Test laufen lassen
 * (Rechtsklick auf die Datei → Run 'BalanceTest'). Die Tabelle steht in der Ausgabe.
 */
class BalanceTest {

    private val levels = 1..40
    private val runsPerLevel = 60

    /** So oft muss der Computer-Spieler mindestens gewinnen. */
    private val minWinRate = 0.5

    @Test
    fun alleLevelSindSchaffbar() {
        println("Level | Züge | Sorten | Ziele        | Gewinnrate | Ø übrige Züge")
        val tooHard = mutableListOf<Int>()
        for (level in levels) {
            val config = LevelGenerator.generate(level, "test")
            var wins = 0
            var leftover = 0
            repeat(runsPerLevel) { run ->
                val end = playLevel(config, Random(level * 1000 + run))
                if (end.status == LevelStatus.WON) {
                    wins++
                    leftover += end.movesLeft
                }
            }
            val rate = wins.toDouble() / runsPerLevel
            if (rate < minWinRate) tooHard += level
            println(
                "%5d | %4d | %6d | %-12s | %9.0f%% | %5.1f".format(
                    level, config.moves, config.kindCount,
                    config.goals.joinToString(" ") { "${it.amount}" } + " Stk",
                    rate * 100, if (wins > 0) leftover.toDouble() / wins else 0.0,
                )
            )
        }
        assertTrue("Zu schwer: Level $tooHard", tooHard.isEmpty())
    }

    private fun playLevel(config: LevelConfig, random: Random): LevelState {
        var state = LevelState.start(config, random)
        while (state.status == LevelStatus.PLAYING) {
            val move = bestMove(state) ?: break
            val result = GameRules.trySwap(state.board, move.first, move.second, config.kindCount, random)
            state = state.afterMove(result as SwapResult.Success)
            state = OpponentAI.act(state, random).state
        }
        return state
    }

    /** Probiert alle Züge aus und nimmt den, der am meisten noch fehlendes Ziel-Futter bringt. */
    private fun bestMove(state: LevelState): Pair<Pos, Pos>? {
        var best: Pair<Pos, Pos>? = null
        var bestValue = -1.0
        val board = state.board
        for (pos in board.positions()) {
            for (other in listOf(Pos(pos.row, pos.col + 1), Pos(pos.row + 1, pos.col))) {
                val result = GameRules.trySwap(board, pos, other, state.config.kindCount, Random(1))
                if (result !is SwapResult.Success) continue
                // Nur den ersten Durchgang bewerten – die Kettenreaktion danach kann der
                // Spieler nicht vorhersehen, weil die nachfallenden Steine zufällig sind.
                val first = result.steps.first().collected
                var value = 0.0
                for (goal in state.config.goals) {
                    val missing = goal.amount - state.progress(goal)
                    value += minOf(missing, first[goal.kind] ?: 0)
                }
                // Widersacher: Eindringling vertreiben, Diebesgut retten, Hindernisse wegräumen
                val step = result.steps.first()
                if (step.intruderChased) value += 3.0
                if (state.thiefTargetId != null && step.removed.any { board[it]?.id == state.thiefTargetId }) value += 2.0
                value += step.obstaclesRemoved.size * 0.5
                value += first.values.sum() * 0.01 // bei Gleichstand: mehr Steine sind besser
                if (value > bestValue) {
                    bestValue = value
                    best = pos to other
                }
            }
        }
        return best
    }
}
