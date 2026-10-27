package com.example.futtersuche.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.futtersuche.data.Campaign
import com.example.futtersuche.data.Food
import com.example.futtersuche.game.Board
import com.example.futtersuche.game.Pos
import com.example.futtersuche.game.Special
import com.example.futtersuche.game.Tile
import kotlin.math.abs

/**
 * Zeichnet das Spielfeld als Schachbrett mit einem Futter-Stein pro Feld.
 * [highlighted] = Felder, die gerade aufleuchten sollen (z. B. ein Treffer kurz vor dem Verschwinden).
 * [markedTileId] = Stein, den der Futterdieb klauen will (wird rot markiert).
 * [onSwipe] wird aufgerufen, wenn der Spieler einen Stein auf ein Nachbarfeld wischt.
 */
@Composable
fun BoardView(
    board: Board,
    campaign: Campaign,
    modifier: Modifier = Modifier,
    highlighted: Set<Pos> = emptySet(),
    markedTileId: Long? = null,
    onSwipe: (from: Pos, to: Pos) -> Unit = { _, _ -> },
) {
    val lightCell = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    val darkCell = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    val highlightCell = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
    val markedCell = MaterialTheme.colorScheme.error.copy(alpha = 0.45f)

    // Die Wisch-Erkennung läuft dauerhaft; mit rememberUpdatedState sieht sie trotzdem
    // immer das aktuelle Feld und die aktuelle onSwipe-Funktion.
    val currentBoard by rememberUpdatedState(board)
    val currentOnSwipe by rememberUpdatedState(onSwipe)

    BoxWithConstraints(
        modifier
            .aspectRatio(board.cols.toFloat() / board.rows)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                var start: Pos? = null
                var dragged = Offset.Zero

                detectDragGestures(
                    onDragStart = { offset ->
                        val cellWidth = size.width.toFloat() / currentBoard.cols
                        val cellHeight = size.height.toFloat() / currentBoard.rows
                        start = Pos(
                            row = (offset.y / cellHeight).toInt().coerceIn(0, currentBoard.rows - 1),
                            col = (offset.x / cellWidth).toInt().coerceIn(0, currentBoard.cols - 1),
                        )
                        dragged = Offset.Zero
                    },
                    onDragEnd = { start = null },
                    onDragCancel = { start = null },
                    onDrag = { change, amount ->
                        change.consume()
                        val from = start ?: return@detectDragGestures
                        dragged += amount

                        // Erst ab ca. einem Drittel Feldbreite zählt es als Wischen.
                        val threshold = size.width.toFloat() / currentBoard.cols / 3f
                        if (dragged.getDistance() < threshold) return@detectDragGestures

                        // Richtung bestimmen: Was ist größer – waagerechte oder senkrechte Bewegung?
                        val to = if (abs(dragged.x) > abs(dragged.y)) {
                            Pos(from.row, from.col + if (dragged.x > 0) 1 else -1)
                        } else {
                            Pos(from.row + if (dragged.y > 0) 1 else -1, from.col)
                        }
                        start = null // pro Wischbewegung nur ein Tausch
                        currentOnSwipe(from, to)
                    },
                )
            }
    ) {
        val cellSize = maxWidth / board.cols

        Column(Modifier.fillMaxSize()) {
            for (row in 0 until board.rows) {
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    for (col in 0 until board.cols) {
                        val pos = Pos(row, col)
                        val tile = board[pos]
                        val intruder = board.intruder?.takeIf { it.pos == pos }
                        val marked = tile != null && tile.id == markedTileId
                        val background = when {
                            pos in highlighted -> highlightCell
                            marked || intruder != null -> markedCell
                            (row + col) % 2 == 0 -> lightCell
                            else -> darkCell
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(background),
                            contentAlignment = Alignment.Center,
                        ) {
                            val size = cellSize * 0.75f
                            when {
                                pos in board.obstacles -> EmojiImage(campaign.obstacle.emoji, size)
                                intruder != null -> EmojiImage(
                                    emoji = campaign.opponent.emoji,
                                    size = size,
                                    badge = intruder.patience.toString(),
                                )
                                tile != null -> TileImage(
                                    tile = tile,
                                    food = campaign.foods[tile.kind],
                                    size = size,
                                    markedBy = if (marked) campaign.opponent.emoji else null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Zeigt EINEN Futter-Stein an. Spezialsteine bekommen unten rechts ein Abzeichen,
 * vom Futterdieb markierte Steine oben links das Widersacher-Emoji.
 */
@Composable
fun TileImage(tile: Tile, food: Food, size: Dp, modifier: Modifier = Modifier, markedBy: String? = null) {
    val badge = when (tile.special) {
        Special.NONE -> null
        Special.LINE_H -> "↔"
        Special.LINE_V -> "↕"
        Special.COLOR_BOMB -> "🌈"
    }
    EmojiImage(emoji = food.emoji, size = size, modifier = modifier, badge = badge, topBadge = markedBy)
}

/**
 * Zeichnet ein Emoji mit optionalen Abzeichen. Das ist die einzige Stelle, an der Emojis
 * auf dem Spielfeld gezeichnet werden – wenn du später eigene Bilder hast, tauschst du nur
 * diese Funktion aus (z. B. gegen Image(painterResource(...), ...)).
 */
@Composable
fun EmojiImage(
    emoji: String,
    size: Dp,
    modifier: Modifier = Modifier,
    badge: String? = null,
    topBadge: String? = null,
) {
    val density = LocalDensity.current
    val fontSize = with(density) { (size * 0.8f).toSp() }
    val badgeSize = with(density) { (size * 0.4f).toSp() }
    val badgeStyle = TextStyle(fontSize = badgeSize, lineHeight = badgeSize, fontWeight = FontWeight.Bold)
    val badgeModifier = Modifier
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.8f))

    Box(modifier.size(size)) {
        Text(
            text = emoji,
            modifier = Modifier.align(Alignment.Center),
            style = TextStyle(fontSize = fontSize, lineHeight = fontSize),
        )
        if (badge != null) {
            Text(
                text = badge,
                modifier = Modifier.align(Alignment.BottomEnd).then(badgeModifier),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = badgeStyle,
            )
        }
        if (topBadge != null) {
            Text(text = topBadge, modifier = Modifier.align(Alignment.TopStart), style = badgeStyle)
        }
    }
}
