package com.example.futtersuche.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CascadeTest {

    private val random = Random(7)

    @Test
    fun steineRutschenNachUndLueckenWerdenAufgefuellt() {
        val board = boardOf(
            "01",
            ".2",
            "3.",
        )
        val fallen = Gravity.apply(board, kindCount = 5, random = random)
        // Spalte 0: 0 und 3 rutschen nach unten, oben kommt ein neuer Stein
        assertEquals(0, fallen[1, 0]?.kind)
        assertEquals(3, fallen[2, 0]?.kind)
        // Spalte 1: 1 und 2 rutschen nach unten
        assertEquals(1, fallen[1, 1]?.kind)
        assertEquals(2, fallen[2, 1]?.kind)
        // Keine Lücken mehr
        assertTrue(fallen.positions().all { fallen[it] != null })
    }

    @Test
    fun nachrutschenErzeugtKettenreaktion() {
        // Die drei 0er in Spalte 0 verschwinden. Die 1 von ganz oben rutscht
        // dadurch in die unterste Zeile – dort entsteht "111" → Kettenreaktion.
        val board = boardOf(
            "123",
            "042",
            "034",
            "011",
        )
        val steps = Cascade.resolve(board, kindCount = 5, random = random)
        assertTrue(steps.size >= 2)
        assertEquals(mapOf(0 to 3), steps[0].collected)
        assertEquals(2, steps[1].chain)
        assertTrue((0..2).all { Pos(3, it) in steps[1].removed })
    }

    @Test
    fun amEndeBleibtKeinTrefferLiegen() {
        repeat(50) { seed ->
            val board = boardOf("123", "042", "034", "011")
            val steps = Cascade.resolve(board, kindCount = 5, random = Random(seed))
            assertFalse(steps.last().afterFall.hasAnyMatch())
            steps.forEachIndexed { i, step -> assertEquals(i + 1, step.chain) }
        }
    }

    @Test
    fun viererReiheErzeugtLinienStein() {
        val board = boardOf(
            "2323",
            "1111",
            "3232",
        )
        val step = Cascade.resolve(board, 5, random, preferredSpecialSpots = setOf(Pos(1, 2))).first()
        val created = step.created[Pos(1, 2)]!!
        assertEquals(Special.LINE_H, created.special)
        assertEquals(1, created.kind)
        // Der neue Spezialstein bleibt liegen und zählt noch nicht, die anderen drei schon
        assertEquals(mapOf(1 to 3), step.collected)
        assertEquals(created, step.afterRemoval[1, 2])
    }

    @Test
    fun senkrechteViererReiheErzeugtSpaltenStein() {
        val board = boardOf(
            "023",
            "032",
            "023",
            "032",
        )
        val step = Cascade.resolve(board, kindCount = 5, random = random).first()
        assertEquals(Special.LINE_V, step.created.values.single().special)
    }

    @Test
    fun fuenferReiheErzeugtFarbbombe() {
        val board = boardOf(
            "23232",
            "00000",
            "32323",
        )
        val step = Cascade.resolve(board, kindCount = 5, random = random).first()
        assertEquals(Special.COLOR_BOMB, step.created.values.single().special)
    }

    @Test
    fun linienSteinLoeschtGanzeZeile() {
        // Zeile 1: Linienstein (Sorte 1), 1, 1, 3 → Treffer löst den Linienstein aus
        val board = boardOf(
            "2324",
            "h1113",
            "3434",
        )
        val step = Cascade.resolve(board, kindCount = 5, random = random).first()
        assertEquals((0 until 4).map { Pos(1, it) }.toSet(), step.removed)
        assertEquals(mapOf(1 to 3, 3 to 1), step.collected) // auch die 3 zählt als gesammelt
    }

    @Test
    fun spezialsteinLoestAnderenSpezialsteinAus() {
        // Zeilenstein oben links löscht Zeile 0, trifft dabei den Spaltenstein → Spalte 1 auch weg
        val board = boardOf(
            "h1v212",
            "3434",
            "4343",
        )
        val step = Cascade.resolve(board, 5, random, forcedRemoval = setOf(Pos(0, 0))).first()
        val expected = (0 until 4).map { Pos(0, it) } + Pos(1, 1) + Pos(2, 1)
        assertEquals(expected.toSet(), step.removed)
    }

    @Test
    fun farbbombeLoeschtAlleSteineDerTauschSorte() {
        val board = boardOf(
            "b0121",
            "2312",
            "1231",
        )
        // Bombe (Sorte 0) mit der 1 rechts daneben tauschen → alle 1er verschwinden
        val result = GameRules.trySwap(board, Pos(0, 0), Pos(0, 1), 5, random)
        assertTrue(result is SwapResult.Success)
        val first = (result as SwapResult.Success).steps.first()
        assertEquals(5, first.collected[1])
        assertEquals(1, first.collected[0]) // die verbrauchte Bombe selbst
        assertTrue(first.created.isEmpty())
    }

    @Test
    fun ohneMoeglichenZugWirdNeuGemischt() {
        val stuck = boardOf(
            "0123",
            "1230",
            "2301",
            "3012",
        )
        assertFalse(MoveFinder.hasPossibleMove(stuck))
        val shuffled = MoveFinder.shuffle(stuck, kindCount = 4, random = random)
        assertTrue(MoveFinder.hasPossibleMove(shuffled))
        assertFalse(shuffled.hasAnyMatch())
    }

    @Test
    fun neuesFeldHatImmerEinenMoeglichenZug() {
        repeat(200) { seed ->
            assertTrue(MoveFinder.hasPossibleMove(BoardGenerator.create(random = Random(seed))))
        }
    }
}
