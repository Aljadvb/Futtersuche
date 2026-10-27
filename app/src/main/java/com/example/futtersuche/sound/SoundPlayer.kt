package com.example.futtersuche.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.futtersuche.data.ProgressStore
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Spielt die im [Synth] erzeugten Töne ab. Lässt sich im Einstellungs-Menü abschalten;
 * die Einstellung wird gespeichert.
 */
class SoundPlayer(private val store: ProgressStore) {

    /** Töne an/aus. "mutableStateOf", damit der Schalter im Menü sich sofort aktualisiert. */
    var enabled by mutableStateOf(store.soundEnabled())
        private set

    private val cache = ConcurrentHashMap<String, ShortArray>()
    private val executor = Executors.newCachedThreadPool()

    fun switchSound(on: Boolean) {
        enabled = on
        store.setSoundEnabled(on)
    }

    fun play(sound: Sound, variant: Int = 1) {
        if (!enabled) return
        val notes = Synth.notesFor(sound, variant)
        val data = cache.getOrPut("$sound-$variant") { Synth.render(notes) }
        executor.execute {
            // Ein Ton darf nie das Spiel abstürzen lassen – Fehler werden ignoriert.
            runCatching {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(Synth.SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(data.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                try {
                    track.write(data, 0, data.size)
                    track.play()
                    Thread.sleep(Synth.durationMs(notes) + 100L)
                } finally {
                    track.release()
                }
            }
        }
    }

    fun release() {
        executor.shutdown()
    }
}
