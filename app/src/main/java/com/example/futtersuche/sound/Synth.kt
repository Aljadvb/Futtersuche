package com.example.futtersuche.sound

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** Alle Geräusche des Spiels. */
enum class Sound { SWAP, INVALID, MATCH, SPECIAL, OPPONENT, CHASED, WIN, LOSE, ACHIEVEMENT }

/**
 * Erzeugt einfache Spiel-Töne direkt im Code (kein Sounddatei nötig).
 * Jeder Ton besteht aus kurzen "Noten", deren Tonhöhe auch gleiten kann (z. B. ein "Wuuup").
 *
 * Möchtest du später echte Sounddateien verwenden, ersetzt du nur den SoundPlayer –
 * der Rest des Spiels ruft weiterhin einfach play(Sound.MATCH) usw. auf.
 */
object Synth {

    const val SAMPLE_RATE = 22050

    enum class Wave { SINE, TRIANGLE, SQUARE }

    /** Eine Note: Tonhöhe gleitet von [startHz] nach [endHz]. Bei 0 Hz ist es eine Pause. */
    data class Note(
        val startHz: Double,
        val endHz: Double = startHz,
        val ms: Int,
        val volume: Double = 0.4,
        val wave: Wave = Wave.TRIANGLE,
    )

    // Notennamen für die Melodien (Hz)
    private const val C5 = 523.25
    private const val D5 = 587.33
    private const val E5 = 659.25
    private const val F5 = 698.46
    private const val G5 = 783.99
    private const val C6 = 1046.5
    private const val E6 = 1318.5
    private const val G6 = 1568.0

    /** Die Noten für ein Geräusch. [variant] = z. B. Kettenstufe beim Treffer (höher = höherer Ton). */
    fun notesFor(sound: Sound, variant: Int = 1): List<Note> = when (sound) {
        Sound.SWAP -> listOf(Note(500.0, 800.0, 60, 0.25, Wave.SINE))
        Sound.INVALID -> listOf(
            Note(320.0, 220.0, 90, 0.3, Wave.SQUARE),
            Note(0.0, ms = 30),
            Note(320.0, 220.0, 90, 0.3, Wave.SQUARE),
        )
        Sound.MATCH -> {
            // Jede Kettenstufe 2 Halbtöne höher
            val base = C5 * 2.0.pow((min(variant, 8) - 1) * 2 / 12.0)
            listOf(Note(base, ms = 60), Note(base * 1.5, ms = 110))
        }
        Sound.SPECIAL -> listOf(C5, E5, G5, C6, E6).map { Note(it, ms = 50, volume = 0.35) }
        Sound.OPPONENT -> listOf(Note(420.0, 140.0, 320, 0.35, Wave.SQUARE))
        Sound.CHASED -> listOf(Note(G5, C6, 120), Note(E6, ms = 140))
        Sound.WIN -> listOf(C5, E5, G5).map { Note(it, ms = 120) } + Note(C6, ms = 380)
        Sound.LOSE -> listOf(G5, F5, E5).map { Note(it, ms = 170, wave = Wave.SINE) } +
            Note(D5, 300.0, 450, wave = Wave.SINE)
        Sound.ACHIEVEMENT -> listOf(C6, E6, G6).map { Note(it, ms = 80, volume = 0.3) } +
            Note(C6 * 2, ms = 250, volume = 0.3)
    }

    /** Wandelt Noten in Audiodaten (16 Bit, mono) um. */
    fun render(notes: List<Note>): ShortArray {
        val out = ArrayList<Short>()
        for (note in notes) {
            val count = note.ms * SAMPLE_RATE / 1000
            val attack = min(count / 4, SAMPLE_RATE * 5 / 1000)   // 5 ms einblenden
            val release = min(count / 2, SAMPLE_RATE * 30 / 1000) // 30 ms ausblenden
            var phase = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = note.startHz + (note.endHz - note.startHz) * t
                phase += freq / SAMPLE_RATE
                val envelope = when {
                    i < attack -> i.toDouble() / attack
                    i > count - release -> (count - i).toDouble() / release
                    else -> 1.0
                }
                val value = if (freq <= 0.0) 0.0 else wave(note.wave, phase) * note.volume * envelope
                out += (value * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
            }
        }
        return out.toShortArray()
    }

    private fun wave(wave: Wave, phase: Double): Double {
        val p = phase - phase.toLong()
        return when (wave) {
            Wave.SINE -> sin(2 * PI * p)
            Wave.TRIANGLE -> 4 * abs(p - 0.5) - 1
            Wave.SQUARE -> if (p < 0.5) 0.6 else -0.6 // etwas leiser, Rechteck klingt sonst schrill
        }
    }

    fun durationMs(notes: List<Note>): Int = notes.sumOf { it.ms }
}
