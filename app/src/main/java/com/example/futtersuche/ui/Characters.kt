package com.example.futtersuche.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.TextUnit

/** Stimmung des Tiers. [badge] = kleines Gesicht, das neben dem Tier erscheint. */
enum class AnimalMood(val badge: String?) {
    IDLE(null),
    HAPPY("😊"),   // Treffer
    EXCITED("🤩"), // Kettenreaktion oder Spezialstein
    FULL("😋"),    // Sieg – satt!
    SAD("😢"),     // Niederlage
}

/** Stimmung des Widersachers. */
enum class OpponentMood(val badge: String?) {
    IDLE(null),
    GLOATING("😈"), // hat Schaden angerichtet
    ANGRY("😠"),    // wurde vertrieben / Futter gerettet / Hindernis weg
}

/**
 * Das Tier mit Reaktionen. [reaction] erhöht sich bei jeder neuen Reaktion,
 * damit das Tier auch bei zwei gleichen Stimmungen nacheinander erneut hüpft.
 *
 * Eigene Bilder später: Statt des Emojis hier ein Image(...) pro Stimmung anzeigen.
 */
@Composable
fun AnimalAvatar(emoji: String, mood: AnimalMood, reaction: Int, size: TextUnit, modifier: Modifier = Modifier) {
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(reaction) {
        if (mood == AnimalMood.HAPPY || mood == AnimalMood.EXCITED || mood == AnimalMood.FULL) {
            bounce.animateTo(if (mood == AnimalMood.EXCITED) 1.35f else 1.2f, tween(120))
            bounce.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    // Satt: gemütliches Wippen, traurig: leicht zusammengesunken und geneigt
    val idle = rememberInfiniteTransition(label = "animal")
    val wobble by idle.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "wobble",
    )
    val rotation = when (mood) {
        AnimalMood.FULL -> wobble * 2
        AnimalMood.SAD -> -12f
        else -> wobble * 0.5f
    }
    val scale = if (mood == AnimalMood.SAD) 0.85f else 1f

    Box(modifier) {
        Text(
            text = emoji,
            fontSize = size,
            modifier = Modifier.graphicsLayer {
                scaleX = bounce.value * scale
                scaleY = bounce.value * scale
                rotationZ = rotation
            },
        )
        mood.badge?.let { Text(it, fontSize = size * 0.45f, modifier = Modifier.align(Alignment.TopEnd)) }
    }
}

/** Der Widersacher mit Reaktionen: grinst und wackelt bei Schaden, ärgert sich beim Vertreiben. */
@Composable
fun OpponentAvatar(emoji: String, mood: OpponentMood, reaction: Int, size: TextUnit, modifier: Modifier = Modifier) {
    val shake = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(reaction) {
        when (mood) {
            OpponentMood.GLOATING -> {
                pop.animateTo(1.3f, tween(120))
                pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            OpponentMood.ANGRY -> {
                repeat(3) {
                    shake.animateTo(14f, tween(60))
                    shake.animateTo(-14f, tween(60))
                }
                shake.animateTo(0f, tween(60))
            }
            OpponentMood.IDLE -> Unit
        }
    }
    Box(modifier) {
        Text(
            text = emoji,
            fontSize = size,
            modifier = Modifier.graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                rotationZ = shake.value
                alpha = if (mood == OpponentMood.IDLE) 0.75f else 1f
            },
        )
        mood.badge?.let { Text(it, fontSize = size * 0.5f, modifier = Modifier.align(Alignment.TopEnd)) }
    }
}
