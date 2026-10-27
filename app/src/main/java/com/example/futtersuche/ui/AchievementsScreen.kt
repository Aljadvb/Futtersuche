package com.example.futtersuche.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.futtersuche.data.Achievement
import com.example.futtersuche.data.Achievements
import com.example.futtersuche.data.Campaigns
import com.example.futtersuche.data.PlayerStats
import com.example.futtersuche.data.ProgressStore

/** Übersicht aller Achievements: erreicht oder noch offen mit Fortschritt. */
@Composable
fun AchievementsScreen(store: ProgressStore, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val stats = remember { store.stats() }
    val reached = remember { store.reachedAchievements() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Zurück") }
            Text(
                text = "🏆 Erfolge",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Achievements.all) { achievement ->
                AchievementRow(achievement, stats, achievement.id in reached)
            }
        }
    }
}

@Composable
private fun AchievementRow(achievement: Achievement, stats: PlayerStats, reached: Boolean) {
    val progress = if (reached) achievement.target else achievement.progress(stats)
    val unlocks = Campaigns.all.filter { it.unlockedBy == achievement.id }

    val colors = MaterialTheme.colorScheme
    CompositionLocalProvider(
        LocalContentColor provides if (reached) colors.onPrimaryContainer else colors.onSurfaceVariant
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (reached) colors.primaryContainer else colors.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(achievement.emoji, fontSize = 36.sp, modifier = Modifier.alpha(if (reached) 1f else 0.4f))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = achievement.title + if (reached) " ✓" else "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(achievement.description, style = MaterialTheme.typography.bodyMedium)
            for (campaign in unlocks) {
                Text(
                    text = "Schaltet frei: ${campaign.animal.emoji} ${campaign.title}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!reached) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress.toFloat() / achievement.target },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("$progress / ${achievement.target}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
}
