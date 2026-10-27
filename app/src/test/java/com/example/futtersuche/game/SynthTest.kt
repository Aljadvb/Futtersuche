package com.example.futtersuche.game

import com.example.futtersuche.sound.Sound
import com.example.futtersuche.sound.Synth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SynthTest {

    @Test
    fun alleToeneHabenPassendeLaengeUndUebersteuernNicht() {
        for (sound in Sound.entries) {
            val notes = Synth.notesFor(sound)
            val samples = Synth.render(notes)
            val expected = Synth.durationMs(notes) * Synth.SAMPLE_RATE / 1000
            assertTrue("$sound ist leer", samples.isNotEmpty())
            assertTrue("$sound hat falsche Länge", abs(samples.size - expected) <= notes.size)
            assertTrue("$sound ist zu kurz oder zu lang", Synth.durationMs(notes) in 50..2000)
            assertTrue("$sound ist stumm", samples.any { abs(it.toInt()) > 1000 })
        }
    }

    @Test
    fun hoehereKetteKlingtHoeher() {
        val chain1 = Synth.notesFor(Sound.MATCH, 1).first().startHz
        val chain3 = Synth.notesFor(Sound.MATCH, 3).first().startHz
        assertTrue(chain3 > chain1)
        assertEquals(chain1, Synth.notesFor(Sound.MATCH, 0).first().startHz * 2.0.pow(2.0 / 12), 0.5)
    }

    private fun Double.pow(x: Double) = Math.pow(this, x)
}
