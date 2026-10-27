package com.example.futtersuche.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.futtersuche.data.Campaign
import com.example.futtersuche.data.ProgressStore
import com.example.futtersuche.game.Board
import com.example.futtersuche.game.GameRules
import com.example.futtersuche.game.LevelGenerator
import com.example.futtersuche.game.LevelState
import com.example.futtersuche.game.LevelStatus
import com.example.futtersuche.game.Pos
import com.example.futtersuche.game.SwapResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Wartezeiten in Millisekunden. In Schritt 7 werden daraus echte Animationen. */
private const val SWAP_DELAY_MS = 200L
private const val HIGHLIGHT_DELAY_MS = 350L
private const val GAP_DELAY_MS = 150L
private const val FALL_DELAY_MS = 250L

/** Infos für den Dialog am Ende eines Levels. */
data class LevelEnd(val won: Boolean, val finalScore: Int, val bonus: Int, val best: Int, val newRecord: Boolean)

/**
 * Steuert ein laufendes Spiel: nimmt Wischbewegungen entgegen, spielt Züge ab,
 * wertet Sieg/Niederlage aus und startet Level neu.
 *
 * Er wird EINMAL pro Kampagne angelegt und lebt so lange wie der Spielbildschirm.
 * Beim Levelwechsel wird nur sein Inhalt ausgetauscht – dadurch arbeitet das Spielfeld
 * immer mit dem aktuellen Level (vorher blieb es nach einem Levelwechsel hängen).
 * Alle Felder mit "mutableStateOf" lösen automatisch ein Neuzeichnen aus.
 */
class GameController(
    private val campaign: Campaign,
    private val store: ProgressStore,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(newLevel(store.currentLevel(campaign.id)))
        private set

    /** Feld während eines Zuges (Zwischenschritte der Animation), sonst null. */
    private var animationBoard by mutableStateOf<Board?>(null)

    var highlighted by mutableStateOf(emptySet<Pos>())
        private set
    var message by mutableStateOf("")
        private set
    var levelEnd by mutableStateOf<LevelEnd?>(null)
        private set

    /** true, solange ein Zug abgespielt wird – dann wird Wischen ignoriert. */
    private var busy = false

    /** Das Feld, das gerade angezeigt werden soll. */
    val shownBoard: Board get() = animationBoard ?: state.board

    fun onSwipe(from: Pos, to: Pos) {
        if (busy || state.status != LevelStatus.PLAYING) return
        when (val result = GameRules.trySwap(state.board, from, to, state.config.kindCount, Random.Default)) {
            SwapResult.NotAllowed -> Unit
            is SwapResult.NoMatch -> scope.launch { playNoMatch(result) }
            is SwapResult.Success -> scope.launch { playSuccess(result) }
        }
    }

    /** "Nochmal": gleiches Level, gleiche Ziele, neues Feld. */
    fun retry() = startLevel(state.config.number)

    fun nextLevel() = startLevel(state.config.number + 1)

    private fun startLevel(number: Int) {
        levelEnd = null
        message = ""
        highlighted = emptySet()
        animationBoard = null
        state = newLevel(number)
    }

    private fun newLevel(number: Int): LevelState = LevelState.start(LevelGenerator.generate(number, campaign.id))

    private suspend fun playNoMatch(result: SwapResult.NoMatch) {
        busy = true
        animationBoard = result.swapped // kurz getauscht zeigen …
        delay(SWAP_DELAY_MS)
        animationBoard = null           // … und zurückspringen (kostet keinen Zug)
        message = "Kein Treffer – das Futter springt zurück."
        busy = false
    }

    private suspend fun playSuccess(result: SwapResult.Success) {
        busy = true
        animationBoard = result.swapped
        delay(SWAP_DELAY_MS)

        // Jeden Durchgang der Kettenreaktion einzeln zeigen
        for (step in result.steps) {
            highlighted = step.removed
            delay(HIGHLIGHT_DELAY_MS)
            animationBoard = step.afterRemoval
            highlighted = emptySet()
            delay(GAP_DELAY_MS)
            animationBoard = step.afterFall
            delay(FALL_DELAY_MS)
        }

        state = state.afterMove(result)
        animationBoard = null
        message = when {
            result.reshuffled -> "Kein Zug mehr möglich – neu gemischt!"
            result.maxChain > 1 -> "Kettenreaktion x${result.maxChain}!"
            else -> ""
        }
        busy = false
        if (state.status != LevelStatus.PLAYING) finishLevel()
    }

    private fun finishLevel() {
        val level = state.config.number
        val won = state.status == LevelStatus.WON
        var newRecord = false
        if (won) {
            newRecord = store.saveBestScore(campaign.id, level, state.finalScore)
            store.reachLevel(campaign.id, level + 1)
        }
        levelEnd = LevelEnd(
            won = won,
            finalScore = state.finalScore,
            bonus = state.leftoverBonus,
            best = store.bestScore(campaign.id, level),
            newRecord = newRecord,
        )
    }
}
