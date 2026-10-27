package com.example.futtersuche.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.futtersuche.data.Campaign
import com.example.futtersuche.data.Food
import com.example.futtersuche.game.Board
import com.example.futtersuche.game.Pos
import com.example.futtersuche.game.Special
import com.example.futtersuche.game.Tile
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Zeichnet das Spielfeld in drei Ebenen:
 *  1. Schachbrett-Hintergrund,
 *  2. Hindernisse und Eindringling,
 *  3. Futter-Steine – jeder Stein gleitet animiert an seinen Platz
 *     (Tauschen, Nachfallen; neue Steine fallen von oben ins Feld).
 *
 * [highlighted] = Felder, deren Steine gerade aufleuchten und verschwinden.
 * [markedTileId] = Stein, den der Futterdieb klauen will.
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
    val colors = MaterialTheme.colorScheme
    val lightCell = colors.secondaryContainer.copy(alpha = 0.55f)
    val darkCell = colors.secondaryContainer.copy(alpha = 0.3f)
    val highlightCell = colors.tertiary.copy(alpha = 0.55f)
    val markedCell = colors.error.copy(alpha = 0.45f)

    // Die Wisch-Erkennung läuft dauerhaft; mit rememberUpdatedState sieht sie trotzdem
    // immer das aktuelle Feld und die aktuelle onSwipe-Funktion.
    val currentBoard by rememberUpdatedState(board)
    val currentOnSwipe by rememberUpdatedState(onSwipe)

    // Welche Steine schon einmal gezeigt wurden – alle anderen sind neu und fallen von oben herein
    val knownIds = remember { mutableSetOf<Long>() }

    BoxWithConstraints(
        modifier
            .aspectRatio(board.cols.toFloat() / board.rows)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface.copy(alpha = 0.9f))
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
        val cellPx = with(LocalDensity.current) { cellSize.toPx() }
        fun offsetOf(row: Int, col: Int) = Offset(col * cellPx, row * cellPx)

        // Neue Steine pro Spalte zählen. Wichtig: Das muss HIER drin passieren (nicht außerhalb von
        // BoxWithConstraints), weil dieser Teil erst später gezeichnet wird – sonst wären die
        // neuen Steine schon als "bekannt" gemerkt und würden nicht hereinfallen.
        val newInColumn = IntArray(board.cols)
        for (pos in board.positions()) {
            val tile = board[pos] ?: continue
            if (tile.id !in knownIds) newInColumn[pos.col]++
        }
        SideEffect { board.positions().mapNotNullTo(knownIds) { board[it]?.id } }

        // Ebene 1: Schachbrett
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until board.rows) {
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    for (col in 0 until board.cols) {
                        val pos = Pos(row, col)
                        val tile = board[pos]
                        val background = when {
                            pos in highlighted -> highlightCell
                            (tile != null && tile.id == markedTileId) || board.intruder?.pos == pos -> markedCell
                            (row + col) % 2 == 0 -> lightCell
                            else -> darkCell
                        }
                        Box(Modifier.weight(1f).fillMaxHeight().background(background))
                    }
                }
            }
        }

        // Ebene 2: Hindernisse und Eindringling (springen beim Auftauchen herein)
        for (pos in board.obstacles) {
            key("obstacle", pos) {
                PopIn(Modifier.offset { offsetOf(pos.row, pos.col).round() }.size(cellSize)) {
                    EmojiImage(campaign.obstacle.emoji, cellSize * 0.75f)
                }
            }
        }
        board.intruder?.let { intruder ->
            key("intruder", intruder.pos) {
                PopIn(Modifier.offset { offsetOf(intruder.pos.row, intruder.pos.col).round() }.size(cellSize)) {
                    EmojiImage(campaign.opponent.emoji, cellSize * 0.75f, badge = intruder.patience.toString())
                }
            }
        }

        // Ebene 3: Futter-Steine
        for (pos in board.positions()) {
            val tile = board[pos] ?: continue
            key(tile.id) {
                val target = offsetOf(pos.row, pos.col)
                // Neue Steine starten oberhalb des Feldes, so viele Reihen höher, wie in der Spalte neu sind
                val start = if (tile.id in knownIds) target else offsetOf(pos.row - newInColumn[pos.col], pos.col)
                TileSprite(
                    tile = tile,
                    food = campaign.foods[tile.kind],
                    target = target,
                    start = start,
                    cellPx = cellPx,
                    cellSize = cellSize,
                    vanishing = pos in highlighted,
                    markedBy = if (tile.id == markedTileId) campaign.opponent.emoji else null,
                )
            }
        }
    }
}

/** Ein Stein, der animiert zu [target] gleitet und beim Verschwinden wegschrumpft. */
@Composable
private fun TileSprite(
    tile: Tile,
    food: Food,
    target: Offset,
    start: Offset,
    cellPx: Float,
    cellSize: Dp,
    vanishing: Boolean,
    markedBy: String?,
) {
    val position = remember { Animatable(start, Offset.VectorConverter) }
    LaunchedEffect(target) {
        val falling = target.y > position.value.y + 1f
        if (falling) {
            // Fallen: je weiter, desto länger (aber nie länger als FALL_MS), mit kleinem Hüpfer
            val rows = (target.y - position.value.y) / cellPx
            val duration = (120 + rows * 45).toInt().coerceAtMost(Timings.FALL_MS)
            position.animateTo(target, tween(duration, easing = EaseOutBounce))
        } else {
            position.animateTo(target, tween(Timings.SWAP_MS, easing = FastOutSlowInEasing))
        }
    }
    val scale by animateFloatAsState(
        targetValue = if (vanishing) 0f else 1f,
        animationSpec = if (vanishing) tween(Timings.VANISH_MS) else spring(Spring.DampingRatioMediumBouncy),
        label = "vanish",
    )
    // Neuer Spezialstein: kurz "aufploppen"
    val specialPop = remember(tile.special) { Animatable(if (tile.isSpecial) 1.4f else 1f) }
    LaunchedEffect(tile.special) { specialPop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy)) }

    Box(
        modifier = Modifier
            .offset { position.value.round() }
            .size(cellSize)
            .graphicsLayer {
                scaleX = scale * specialPop.value
                scaleY = scale * specialPop.value
            }
            .padding(cellSize * 0.06f)
            .clip(RoundedCornerShape(cellSize * 0.25f))
            .background(
                if (tile.isSpecial) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
            ),
        contentAlignment = Alignment.Center,
    ) {
        TileImage(tile = tile, food = food, size = cellSize * 0.75f, markedBy = markedBy)
    }
}

/** Lässt den Inhalt beim ersten Erscheinen mit einem Hüpfer hereinploppen. */
@Composable
private fun PopIn(modifier: Modifier, content: @Composable () -> Unit) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) }
    Box(modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }, contentAlignment = Alignment.Center) {
        content()
    }
}

private fun Offset.round() = IntOffset(x.roundToInt(), y.roundToInt())

/**
 * Zeigt EINEN Futter-Stein an. Spezialsteine bekommen unten rechts ein Abzeichen,
 * vom Futterdieb markierte Steine oben links das (pulsierende) Widersacher-Emoji.
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

    Box(modifier.size(size)) {
        Text(
            text = emoji,
            modifier = Modifier.align(Alignment.Center),
            style = TextStyle(fontSize = fontSize, lineHeight = fontSize),
        )
        if (badge != null) {
            Text(
                text = badge,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f)),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = badgeStyle,
            )
        }
        if (topBadge != null) {
            val pulse = rememberInfiniteTransition(label = "mark")
            val scale by pulse.animateFloat(
                initialValue = 0.85f, targetValue = 1.2f,
                animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "pulse",
            )
            Text(
                text = topBadge,
                modifier = Modifier.align(Alignment.TopStart).graphicsLayer { scaleX = scale; scaleY = scale },
                style = badgeStyle,
            )
        }
    }
}
