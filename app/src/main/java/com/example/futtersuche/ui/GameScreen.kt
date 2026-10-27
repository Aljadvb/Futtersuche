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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.futtersuche.data.Campaign
import com.example.futtersuche.data.ProgressStore

/** Der Spielbildschirm. Die ganze Spiel-Logik steckt im [GameController], hier wird nur angezeigt. */
@Composable
fun GameScreen(campaign: Campaign, store: ProgressStore, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val controller = remember(campaign) { GameController(campaign, store, scope) }

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

        GameHud(campaign = campaign, state = controller.state)
        Spacer(Modifier.height(16.dp))

        BoardView(
            board = controller.shownBoard,
            foods = campaign.foods,
            highlighted = controller.highlighted,
            onSwipe = controller::onSwipe,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = controller.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }

    controller.levelEnd?.let { end ->
        LevelEndDialog(
            campaign = campaign,
            end = end,
            onRetry = controller::retry,
            onNext = controller::nextLevel,
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
