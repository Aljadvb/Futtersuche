package com.example.futtersuche.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.futtersuche.data.Campaign
import com.example.futtersuche.game.BoardGenerator

/** Der Spielbildschirm. In Schritt 1 zeigt er nur das Spielfeld und einen Knopf zum Neu-Füllen. */
@Composable
fun GameScreen(campaign: Campaign, modifier: Modifier = Modifier) {
    val kindCount = 5 // ab Schritt 4 kommt dieser Wert aus der Level-Generierung

    // "remember" + "mutableStateOf": Compose merkt sich das Feld und zeichnet neu, wenn es sich ändert.
    var board by remember { mutableStateOf(BoardGenerator.create(kindCount = kindCount)) }

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
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        Button(onClick = { board = BoardGenerator.create(kindCount = kindCount) }) {
            Text("Neues Feld")
        }
    }
}
