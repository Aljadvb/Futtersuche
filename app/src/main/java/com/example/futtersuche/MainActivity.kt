package com.example.futtersuche

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.futtersuche.data.Campaigns
import com.example.futtersuche.ui.GameScreen
import com.example.futtersuche.ui.theme.FuttersucheTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FuttersucheTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Vorerst starten wir direkt in Hoppels Kampagne.
                    // Den Auswahlbildschirm bauen wir in Schritt 6.
                    GameScreen(
                        campaign = Campaigns.hoppel,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
