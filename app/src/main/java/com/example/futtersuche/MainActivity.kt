package com.example.futtersuche

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.futtersuche.data.Campaigns
import com.example.futtersuche.data.ProgressStore
import com.example.futtersuche.ui.AchievementsScreen
import com.example.futtersuche.ui.CampaignSelectScreen
import com.example.futtersuche.ui.GameScreen
import com.example.futtersuche.ui.theme.FuttersucheTheme

/** Die Bildschirme der App. Für eine Kampagne wird ihre ID angehängt: "game:garten". */
private const val SCREEN_SELECT = "select"
private const val SCREEN_ACHIEVEMENTS = "achievements"
private const val SCREEN_GAME_PREFIX = "game:"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ProgressStore(applicationContext)
        // Falls schon vorher Fortschritt da war: Achievements nachträglich prüfen (ohne Einblendung)
        store.unlockNewAchievements()

        setContent {
            FuttersucheTheme {
                // rememberSaveable: Der aktuelle Bildschirm bleibt z. B. beim Umschalten auf Dark Mode erhalten
                var screen by rememberSaveable { mutableStateOf(SCREEN_SELECT) }
                val goToSelect = { screen = SCREEN_SELECT }

                // Zurück-Taste/-Geste führt immer zur Kampagnen-Auswahl (dort beendet sie die App)
                BackHandler(enabled = screen != SCREEN_SELECT, onBack = goToSelect)

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val modifier = Modifier.padding(innerPadding)
                    val campaign = Campaigns.byId(screen.removePrefix(SCREEN_GAME_PREFIX))
                    when {
                        screen == SCREEN_ACHIEVEMENTS -> AchievementsScreen(
                            store = store,
                            onBack = goToSelect,
                            modifier = modifier,
                        )

                        screen.startsWith(SCREEN_GAME_PREFIX) && campaign != null -> GameScreen(
                            campaign = campaign,
                            store = store,
                            onBack = goToSelect,
                            modifier = modifier,
                        )

                        else -> CampaignSelectScreen(
                            store = store,
                            onPlay = { screen = SCREEN_GAME_PREFIX + it.id },
                            onShowAchievements = { screen = SCREEN_ACHIEVEMENTS },
                            modifier = modifier,
                        )
                    }
                }
            }
        }
    }
}
