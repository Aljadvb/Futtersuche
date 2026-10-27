package com.example.futtersuche.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.futtersuche.sound.SoundPlayer

/** Das Einstellungs-Menü. Bisher gibt es nur Töne an/aus – weitere Schalter kommen einfach hier dazu. */
@Composable
fun SettingsDialog(sound: SoundPlayer, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚙️ Einstellungen") },
        text = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (sound.enabled) "🔊 Töne" else "🔇 Töne", Modifier.weight(1f))
                Switch(checked = sound.enabled, onCheckedChange = sound::switchSound)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
    )
}
