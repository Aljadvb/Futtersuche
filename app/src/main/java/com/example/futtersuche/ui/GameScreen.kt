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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.futtersuche.data.Campaign
import com.example.futtersuche.data.Food
import com.example.futtersuche.game.BoardGenerator
import com.example.futtersuche.game.GameRules
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

/** Der Spielbildschirm: Spielfeld, Wischen, Treffer, Nachrutschen und Kettenreaktionen. */
@Composable
fun GameScreen(campaign: Campaign, modifier: Modifier = Modifier) {
    val kindCount = 5 // ab Schritt 4 kommt dieser Wert aus der Level-Generierung

    // "remember" + "mutableStateOf": Compose merkt sich diese Werte und zeichnet neu, wenn sie sich ändern.
    var board by remember { mutableStateOf(BoardGenerator.create(kindCount = kindCount)) }
    var highlighted by remember { mutableStateOf(emptySet<Pos>()) }
    var busy by remember { mutableStateOf(false) } // true, solange ein Tausch abläuft
    var message by remember { mutableStateOf("Wische ein Futter auf ein Nachbarfeld!") }
    val scope = rememberCoroutineScope()

    fun onSwipe(from: Pos, to: Pos) {
        if (busy) return
        when (val result = GameRules.trySwap(board, from, to, kindCount, Random.Default)) {
            SwapResult.NotAllowed -> Unit

            is SwapResult.NoMatch -> scope.launch {
                busy = true
                val before = board
                board = result.swapped // kurz getauscht zeigen …
                delay(SWAP_DELAY_MS)
                board = before          // … und zurückspringen
                message = "Kein Treffer – das Futter springt zurück."
                busy = false
            }

            is SwapResult.Success -> scope.launch {
                busy = true
                board = result.swapped
                delay(SWAP_DELAY_MS)

                // Jeden Durchgang der Kettenreaktion einzeln zeigen
                for (step in result.steps) {
                    highlighted = step.removed // Treffer aufleuchten lassen
                    delay(HIGHLIGHT_DELAY_MS)
                    board = step.afterRemoval  // Lücken (und neue Spezialsteine) zeigen
                    highlighted = emptySet()
                    delay(GAP_DELAY_MS)
                    board = step.afterFall     // nachgerutscht und aufgefüllt
                    delay(FALL_DELAY_MS)
                }

                message = describeCollected(result.collected, campaign.foods)
                if (result.maxChain > 1) message += "\nKettenreaktion x${result.maxChain}!"
                if (result.reshuffled) {
                    message += "\nKein Zug mehr möglich – neu gemischt!"
                    board = result.final
                }
                busy = false
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
            text = "${campaign.animal.emoji} ${campaign.title}",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))

        BoardView(
            board = board,
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

        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            if (!busy) {
                board = BoardGenerator.create(kindCount = kindCount)
                message = "Neues Feld! Wische ein Futter auf ein Nachbarfeld."
            }
        }) {
            Text("Neues Feld")
        }
    }
}

/** Baut einen Text wie "Gesammelt: 3 Karotten, 1 Apfel". */
private fun describeCollected(countPerKind: Map<Int, Int>, foods: List<Food>): String {
    val parts = countPerKind.map { (kind, count) ->
        val food = foods[kind]
        "$count ${if (count == 1) food.name else food.pluralName}"
    }
    return "Gesammelt: " + parts.joinToString(", ")
}
