package com.example.futtersuche.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** So lange bleibt eine Einblendung sichtbar (Millisekunden). */
private const val TOAST_DURATION_MS = 3000L

/**
 * Kurze Einblendung am oberen Rand (z. B. "Erfolg erreicht!").
 * Verschwindet nach ein paar Sekunden von selbst oder beim Antippen.
 */
@Composable
fun ToastBanner(text: String?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    // Letzten Text merken, damit er beim Ausblenden noch zu sehen ist
    var shownText by remember { mutableStateOf("") }
    if (text != null) shownText = text

    LaunchedEffect(text) {
        if (text != null) {
            delay(TOAST_DURATION_MS)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = text != null,
        modifier = modifier,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .clickable(onClick = onDismiss)
                .padding(16.dp),
        ) {
            Text(
                text = shownText,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}
