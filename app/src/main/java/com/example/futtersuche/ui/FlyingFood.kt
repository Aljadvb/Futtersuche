package com.example.futtersuche.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.example.futtersuche.data.Food
import com.example.futtersuche.game.Pos
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/** Ein Stück Futter, das gerade vom Feld [from] zum Tier fliegt. */
data class Flight(val id: Long, val kind: Int, val from: Pos, val delayMs: Int)

/**
 * Ebene über dem ganzen Spielbildschirm, auf der gesammeltes Futter im Bogen zum Tier fliegt.
 * Alle Positionen sind relativ zu dieser Ebene.
 * [board] = Rechteck des Spielfelds, [animal] = Mittelpunkt des Tiers.
 */
@Composable
fun FlyingFoodLayer(
    flights: List<Flight>,
    foods: List<Food>,
    board: Rect?,
    boardCols: Int,
    animal: Offset?,
    onDone: (Long) -> Unit,
) {
    if (board == null || animal == null) return
    val cell = board.width / boardCols
    val halfEmoji = with(LocalDensity.current) { 14.sp.toPx() }

    Box {
        for (flight in flights) {
            key(flight.id) {
                val progress = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    delay(flight.delayMs.toLong())
                    progress.animateTo(1f, tween(Timings.FLIGHT_MS, easing = FastOutSlowInEasing))
                    onDone(flight.id)
                }
                val start = Offset(board.left + (flight.from.col + 0.5f) * cell, board.top + (flight.from.row + 0.5f) * cell)
                val p = progress.value
                // Gerade Linie plus Bogen nach oben
                val point = lerp(start, animal, p) - Offset(0f, sin(PI * p).toFloat() * cell * 1.5f)
                Text(
                    text = foods[flight.kind].emoji,
                    fontSize = 28.sp,
                    modifier = Modifier
                        .offset { IntOffset((point.x - halfEmoji).roundToInt(), (point.y - halfEmoji).roundToInt()) }
                        .graphicsLayer {
                            val s = 1.2f - 0.6f * p
                            scaleX = s
                            scaleY = s
                            alpha = if (p == 0f) 0f else 1f
                        },
                )
            }
        }
    }
}
