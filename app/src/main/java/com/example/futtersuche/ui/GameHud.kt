package com.example.futtersuche.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.futtersuche.data.Campaign
import com.example.futtersuche.game.LevelState

/**
 * Obere Anzeige: Level, Züge, Punkte – darunter das Tier mit seinem Futterwunsch
 * (Fortschritt pro Ziel) und, falls aktiv, der Widersacher.
 * [onAnimalPositioned] meldet, wo das Tier sitzt (Ziel für fliegendes Futter).
 */
@Composable
fun GameHud(
    campaign: Campaign,
    state: LevelState,
    animalMood: AnimalMood,
    animalReaction: Int,
    opponentMood: OpponentMood,
    opponentReaction: Int,
    onAnimalPositioned: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Alle Texte ohne eigene Farbe passen zum Hintergrund (auch im Dark Mode)
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(12.dp),
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
                    fontWeight = FontWeight.ExtraBold,
                    color = if (state.movesLeft <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Text("⭐ ${state.score}", style = MaterialTheme.typography.titleMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimalAvatar(
                    emoji = campaign.animal.emoji,
                    mood = animalMood,
                    reaction = animalReaction,
                    size = 52.sp,
                    modifier = Modifier.onGloballyPositioned { onAnimalPositioned(it.boundsInRoot().center) },
                )
                Spacer(Modifier.width(8.dp))

                // Sprechblase mit dem Futterwunsch
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimaryContainer) {
                        Text(
                            text = "${campaign.animal.name} möchte:",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        for (goal in state.config.goals) {
                            val food = campaign.foods[goal.kind]
                            val done = state.progress(goal) >= goal.amount
                            Text(
                                text = "${food.emoji} ${food.pluralName}: ${state.progress(goal)}/${goal.amount}" +
                                    if (done) " ✓" else "",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (done) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }

                if (state.config.opponent.behaviors().isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    OpponentAvatar(
                        emoji = campaign.opponent.emoji,
                        mood = opponentMood,
                        reaction = opponentReaction,
                        size = 36.sp,
                    )
                }
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
