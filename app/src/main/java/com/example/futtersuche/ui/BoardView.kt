package com.example.futtersuche.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.futtersuche.data.Food
import com.example.futtersuche.game.Board

/** Zeichnet das Spielfeld als Schachbrett mit einem Futter-Stein pro Feld. */
@Composable
fun BoardView(board: Board, foods: List<Food>, modifier: Modifier = Modifier) {
    val lightCell = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    val darkCell = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)

    BoxWithConstraints(
        modifier
            .aspectRatio(board.cols.toFloat() / board.rows)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val cellSize = maxWidth / board.cols

        Column(Modifier.fillMaxSize()) {
            for (row in 0 until board.rows) {
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    for (col in 0 until board.cols) {
                        val tile = board[row, col]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if ((row + col) % 2 == 0) lightCell else darkCell),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (tile != null) {
                                TileImage(food = foods[tile.kind], size = cellSize * 0.75f)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Zeigt EIN Futter an. Das ist die einzige Stelle, an der Emojis gezeichnet werden –
 * wenn du später eigene Bilder hast, tauschst du nur diese Funktion aus
 * (z. B. gegen Image(painterResource(food.imageRes), ...)).
 */
@Composable
fun TileImage(food: Food, size: Dp, modifier: Modifier = Modifier) {
    val fontSize = with(LocalDensity.current) { (size * 0.8f).toSp() }
    Text(
        text = food.emoji,
        modifier = modifier,
        style = TextStyle(fontSize = fontSize, lineHeight = fontSize),
    )
}
