package com.example.futtersuche.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.futtersuche.data.Campaign
import com.example.futtersuche.data.ProgressStore
import com.example.futtersuche.game.Board
import com.example.futtersuche.game.GameRules
import com.example.futtersuche.game.LevelGenerator
import com.example.futtersuche.game.LevelState
import com.example.futtersuche.game.LevelStatus
import com.example.futtersuche.game.Pos
import com.example.futtersuche.game.SwapResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Wartezeiten in Millisekunden. In Schritt 7 werden daraus echte Animationen. */
private const val SWAP_DELAY_MS = 200L
private const val HIGHLIGHT_DELAY_MS = 350L
private const val GAP_DELAY_MS = 150L
private const val FALL_DELAY_MS = 250L

/** Infos für den Dialog am Ende eines Levels. */
private data class LevelEnd(val won: Boolean, val finalScore: Int, val bonus: Int, val best: Int, val newRecord: Boolean)

/** Der Spielbildschirm: ein Level spielen, von Anfang bis Sieg oder Niederlage. */
@Composable
fun GameScreen(campaign: Campaign, store: ProgressStore, modifier: Modifier = Modifier) {
    var levelNumber by remember { mutableIntStateOf(store.currentLevel(campaign.id)) }
    var attempt by remember { mutableIntStateOf(0) } // erhöht sich bei "Nochmal" → neues Feld, gleiche Ziele

    // "remember(Schlüssel)": Ändert sich Level oder Versuch, wird ein neuer Level-Stand erzeugt.
    var state by remember(levelNumber, attempt) {
        mutableStateOf(LevelState.start(LevelGenerator.generate(levelNumber, campaign.id)))
    }
    var animationBoard by remember { mutableStateOf<Board?>(null) } // Feld während eines Zuges
    var highlighted by remember { mutableStateOf(emptySet<Pos>()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var levelEnd by remember { mutableStateOf<LevelEnd?>(null) }
    val scope = rememberCoroutineScope()

    fun finishLevel(finished: LevelState) {
        val won = finished.status == LevelStatus.WON
        var newRecord = false
        if (won) {
            newRecord = store.saveBestScore(campaign.id, levelNumber, finished.finalScore)
            store.reachLevel(campaign.id, levelNumber + 1)
        }
        levelEnd = LevelEnd(
            won = won,
            finalScore = finished.finalScore,
            bonus = finished.leftoverBonus,
            best = store.bestScore(campaign.id, levelNumber),
            newRecord = newRecord,
        )
    }

    fun startLevel(number: Int) {
        levelEnd = null
        message = ""
        if (number == levelNumber) attempt++ else levelNumber = number
    }

    fun onSwipe(from: Pos, to: Pos) {
        if (busy || state.status != LevelStatus.PLAYING) return
        when (val result = GameRules.trySwap(state.board, from, to, state.config.kindCount, Random.Default)) {
            SwapResult.NotAllowed -> Unit

            is SwapResult.NoMatch -> scope.launch {
                busy = true
                animationBoard = result.swapped // kurz getauscht zeigen …
                delay(SWAP_DELAY_MS)
                animationBoard = null           // … und zurückspringen (kostet keinen Zug)
                message = "Kein Treffer – das Futter springt zurück."
                busy = false
            }

            is SwapResult.Success -> scope.launch {
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
                if (state.status != LevelStatus.PLAYING) finishLevel(state)
            }
        }
    }

    val background = if (isSystemInDarkTheme()) campaign.backgroundDark else campaign.backgroundLight

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = campaign.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))

        GameHud(campaign = campaign, state = state)
        Spacer(Modifier.height(16.dp))

        BoardView(
            board = animationBoard ?: state.board,
            foods = campaign.foods,
            highlighted = highlighted,
            onSwipe = ::onSwipe,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }

    levelEnd?.let { end ->
        LevelEndDialog(
            campaign = campaign,
            end = end,
            onRetry = { startLevel(levelNumber) },
            onNext = { startLevel(levelNumber + 1) },
        )
    }
}

@Composable
private fun LevelEndDialog(campaign: Campaign, end: LevelEnd, onRetry: () -> Unit, onNext: () -> Unit) {
    val animal = campaign.animal
    AlertDialog(
        onDismissRequest = {}, // Dialog lässt sich nur über die Knöpfe schließen
        title = {
            Text(if (end.won) "${animal.emoji} ${animal.name} ist satt!" else "${animal.emoji} Oh nein!")
        },
        text = {
            Text(
                if (end.won) {
                    buildString {
                        append("Geschafft!\n\n")
                        if (end.bonus > 0) append("Bonus für übrige Züge: ${end.bonus}\n")
                        append("Punkte: ${end.finalScore}\n")
                        append(if (end.newRecord) "Neuer Bestwert! 🏆" else "Bestwert: ${end.best}")
                    }
                } else {
                    "Die Züge sind aufgebraucht. ${animal.name} hat noch Hunger – versuch es nochmal!"
                }
            )
        },
        confirmButton = {
            if (end.won) {
                TextButton(onClick = onNext) { Text("Nächstes Level") }
            } else {
                TextButton(onClick = onRetry) { Text("Nochmal") }
            }
        },
        dismissButton = {
            if (end.won) TextButton(onClick = onRetry) { Text("Nochmal") }
        },
    )
}
