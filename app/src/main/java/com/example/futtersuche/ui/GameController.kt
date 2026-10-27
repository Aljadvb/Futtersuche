package com.example.futtersuche.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.futtersuche.data.Campaign
import com.example.futtersuche.data.Campaigns
import com.example.futtersuche.data.ProgressStore
import com.example.futtersuche.game.Board
import com.example.futtersuche.game.CascadeStep
import com.example.futtersuche.game.GameRules
import com.example.futtersuche.game.LevelGenerator
import com.example.futtersuche.game.LevelState
import com.example.futtersuche.game.LevelStatus
import com.example.futtersuche.game.OpponentAI
import com.example.futtersuche.game.Pos
import com.example.futtersuche.game.SaveCodec
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
private const val OPPONENT_DELAY_MS = 400L

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
    var state by mutableStateOf(loadOrStartLevel())
        private set

    /** Feld während eines Zuges (Zwischenschritte der Animation), sonst null. */
    private var animationBoard by mutableStateOf<Board?>(null)

    var highlighted by mutableStateOf(emptySet<Pos>())
        private set
    var message by mutableStateOf("")
        private set
    var levelEnd by mutableStateOf<LevelEnd?>(null)
        private set

    /** Erklär-Hinweise, die noch gezeigt werden müssen (der erste wird angezeigt). */
    private var pendingHints by mutableStateOf(hintsFor(state))

    /** Der Hinweis, der gerade angezeigt werden soll (null = keiner). */
    val hint: OpponentBehavior? get() = pendingHints.firstOrNull()

    /** Kurze Einblendungen (z. B. "Erfolg erreicht!"), die nacheinander gezeigt werden. */
    private var pendingToasts by mutableStateOf(emptyList<String>())

    /** Die Einblendung, die gerade sichtbar ist (null = keine). */
    val toast: String? get() = pendingToasts.firstOrNull()

    fun dismissToast() {
        pendingToasts = pendingToasts.drop(1)
    }

    /** true, solange ein Zug abgespielt wird – dann wird Wischen ignoriert. */
    private var busy = false

    /** Das Feld, das gerade angezeigt werden soll. */
    val shownBoard: Board get() = animationBoard ?: state.board

    fun onSwipe(from: Pos, to: Pos) {
        if (busy || hint != null || state.status != LevelStatus.PLAYING) return
        when (val result = GameRules.trySwap(state.board, from, to, state.config.kindCount, Random.Default)) {
            SwapResult.NotAllowed -> Unit
            is SwapResult.NoMatch -> scope.launch { playNoMatch(result) }
            is SwapResult.Success -> scope.launch { playSuccess(result) }
        }
    }

    /** Hinweis wurde gelesen → merken und den nächsten zeigen. */
    fun dismissHint() {
        val current = hint ?: return
        store.markHintSeen(current.key)
        pendingHints = pendingHints.drop(1)
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
        saveProgress(state)
        pendingHints = hintsFor(state)
    }

    /** Verhaltensweisen des Widersachers in diesem Level, deren Erklärung noch nie gezeigt wurde. */
    private fun hintsFor(level: LevelState): List<OpponentBehavior> =
        level.config.opponent.behaviors().filter { !store.hintSeen(it.key) }

    private fun newLevel(number: Int): LevelState = LevelState.start(LevelGenerator.generate(number, campaign.id))

    /** Setzt ein gespeichertes, noch laufendes Level fort – sonst beginnt das aktuelle Level neu. */
    private fun loadOrStartLevel(): LevelState {
        val saved = store.loadGame(campaign.id)?.let { SaveCodec.decode(it, campaign.id) }
        if (saved != null && saved.status == LevelStatus.PLAYING) return saved
        return newLevel(store.currentLevel(campaign.id)).also(::saveProgress)
    }

    /** Sichert den Stand, solange das Level läuft; danach wird der Spielstand gelöscht. */
    private fun saveProgress(level: LevelState) {
        if (level.status == LevelStatus.PLAYING) {
            store.saveGame(campaign.id, SaveCodec.encode(level))
        } else {
            store.clearGame(campaign.id)
        }
    }

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

        playSteps(result.steps)

        state = state.afterMove(result)
        recordStats(result.steps)
        animationBoard = null
        val lines = mutableListOf<String>()
        if (result.maxChain > 1) lines += "Kettenreaktion x${result.maxChain}!"
        playerHitsText(campaign, result.steps)?.let { lines += it }
        if (result.reshuffled) lines += "Kein Zug mehr möglich – neu gemischt!"
        message = lines.joinToString("\n")

        // Jetzt ist der Widersacher dran (nur solange das Level noch läuft)
        if (state.status == LevelStatus.PLAYING) playOpponent()
        saveProgress(state)

        busy = false
        if (state.status != LevelStatus.PLAYING) finishLevel()
    }

    private suspend fun playOpponent() {
        val turn = OpponentAI.act(state, Random.Default)
        if (turn.events.isEmpty() && turn.steps.isEmpty() && !turn.reshuffled) {
            state = turn.state
            return
        }
        delay(OPPONENT_DELAY_MS)
        val lines = turn.events.map { opponentEventText(campaign, it) }.toMutableList()
        if (turn.reshuffled) lines += "Kein Zug mehr möglich – neu gemischt!"
        message = (listOf(message) + lines).filter { it.isNotBlank() }.joinToString("\n")

        animationBoard = turn.afterActions
        // Die Dieb-Markierung schon jetzt zeigen
        state = state.copy(thiefTargetId = turn.state.thiefTargetId)
        delay(FALL_DELAY_MS)
        playSteps(turn.steps)
        state = turn.state
        recordStats(turn.steps)
        animationBoard = null
    }

    /** Zählt die Statistik für die Achievements mit und meldet neu erreichte. */
    private fun recordStats(steps: List<CascadeStep>) {
        for (step in steps) {
            for ((kind, count) in step.collected) store.addFoodCollected(campaign.id, kind, count)
            store.addObstaclesRemoved(step.obstaclesRemoved.size)
            if (step.intruderChased) store.addIntrudersChased(1)
        }
        store.recordChain(steps.maxOfOrNull { it.chain } ?: 0)
        announceNewAchievements()
    }

    private fun announceNewAchievements() {
        val texts = mutableListOf<String>()
        for (achievement in store.unlockNewAchievements()) {
            texts += "🏆 Erfolg erreicht: ${achievement.emoji} ${achievement.title}"
            for (unlocked in Campaigns.all.filter { it.unlockedBy == achievement.id }) {
                texts += "🎉 Neue Kampagne freigeschaltet: ${unlocked.animal.emoji} ${unlocked.title}"
            }
        }
        pendingToasts = pendingToasts + texts
    }

    /** Spielt die Durchgänge einer Kettenreaktion nacheinander ab. */
    private suspend fun playSteps(steps: List<CascadeStep>) {
        for (step in steps) {
            highlighted = step.removed
            delay(HIGHLIGHT_DELAY_MS)
            animationBoard = step.afterRemoval
            highlighted = emptySet()
            delay(GAP_DELAY_MS)
            animationBoard = step.afterFall
            delay(FALL_DELAY_MS)
        }
    }

    private fun finishLevel() {
        val level = state.config.number
        val won = state.status == LevelStatus.WON
        var newRecord = false
        if (won) {
            newRecord = store.saveBestScore(campaign.id, level, state.finalScore)
            store.reachLevel(campaign.id, level + 1)
            if (state.movesLeft >= 5) store.addWinWithFiveMovesLeft()
            announceNewAchievements()
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
