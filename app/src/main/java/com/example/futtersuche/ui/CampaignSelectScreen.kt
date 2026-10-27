package com.example.futtersuche.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.futtersuche.data.Achievements
import com.example.futtersuche.data.Campaign
import com.example.futtersuche.data.Campaigns
import com.example.futtersuche.data.ProgressStore

/** Startbildschirm: Kampagne wählen. Gesperrte Kampagnen sind ausgegraut und zeigen, was sie freischaltet. */
@Composable
fun CampaignSelectScreen(
    store: ProgressStore,
    onPlay: (Campaign) -> Unit,
    onShowAchievements: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reached = remember { store.reachedAchievements() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Futtersuche",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Wen möchtest du füttern?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(Campaigns.all) { campaign ->
                CampaignCard(
                    campaign = campaign,
                    unlocked = campaign.isUnlocked(reached),
                    level = store.currentLevel(campaign.id),
                    onClick = { onPlay(campaign) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onShowAchievements) {
            Text("🏆 Erfolge (${reached.size}/${Achievements.all.size})")
        }
    }
}

@Composable
private fun CampaignCard(campaign: Campaign, unlocked: Boolean, level: Int, onClick: () -> Unit) {
    val background = if (isSystemInDarkTheme()) campaign.backgroundDark else campaign.backgroundLight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .clickable(enabled = unlocked, onClick = onClick)
            .alpha(if (unlocked) 1f else 0.45f)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(campaign.animal.emoji, fontSize = 48.sp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = campaign.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = campaign.foods.joinToString(" ") { it.emoji } + "  vs. " + campaign.opponent.emoji,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (unlocked) {
                Text(
                    text = "Level $level",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                val achievement = campaign.unlockedBy?.let { Achievements.byId(it) }
                Text(
                    text = "🔒 Freischalten: " + (achievement?.let { "${it.emoji} ${it.title} – ${it.description}" } ?: "?"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
