package com.example.futtersuche.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.futtersuche.data.Campaign
import com.example.futtersuche.game.LevelState

/** Obere Anzeige: Level, Züge, Punkte, Futterwunsch und Fortschritt pro Ziel. */
@Composable
fun GameHud(campaign: Campaign, state: LevelState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Level ${state.config.number}", style = MaterialTheme.typography.titleMedium)
            Text(
                "Züge: ${state.movesLeft}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (state.movesLeft <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text("Punkte: ${state.score}", style = MaterialTheme.typography.titleMedium)
        }

        Text(
            text = "${campaign.animal.emoji} " + wishText(campaign, state),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )

        // Ziele untereinander, damit auch drei Ziele auf schmale Bildschirme passen
        Column(
            modifier = Modifier.padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            for (goal in state.config.goals) {
                val food = campaign.foods[goal.kind]
                val done = state.progress(goal) >= goal.amount
                Text(
                    text = "${food.emoji} ${food.pluralName}: ${state.progress(goal)}/${goal.amount}" +
                        if (done) " ✓" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (done) FontWeight.Bold else FontWeight.Normal,
                    color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** "Hoppel möchte 12 Karotten und 8 Äpfel" */
fun wishText(campaign: Campaign, state: LevelState): String {
    val parts = state.config.goals.map { "${it.amount} ${campaign.foods[it.kind].pluralName}" }
    val list = if (parts.size == 1) parts[0] else parts.dropLast(1).joinToString(", ") + " und " + parts.last()
    return "${campaign.animal.name} möchte $list"
}
