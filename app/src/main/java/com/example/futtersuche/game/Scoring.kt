package com.example.futtersuche.game

/** Punkteregeln an einem Ort. */
object Scoring {
    const val POINTS_3 = 30
    const val POINTS_4 = 60
    const val POINTS_5 = 100

    /** Punkte für jeden Stein, den ein Spezialstein zusätzlich entfernt. */
    const val POINTS_PER_SPECIAL_STONE = 10

    /** Bonuspunkte pro übrigem Zug nach einem Sieg. */
    const val POINTS_PER_LEFTOVER_MOVE = 50

    /**
     * Multiplikator für Kettenreaktionen: 1. Durchgang x1, 2. x1,5, 3. x2, 4. x2,5 …
     */
    fun chainMultiplier(chain: Int): Double = 1.0 + (chain - 1) * 0.5

    fun pointsForMatch(size: Int): Int = when {
        size >= 5 -> POINTS_5
        size == 4 -> POINTS_4
        else -> POINTS_3
    }

    /** Punkte für einen ganzen Zug inklusive aller Kettenreaktionen. */
    fun pointsFor(result: SwapResult.Success): Int = result.steps.sumOf { step ->
        val inMatches = step.matches.flatMap { it.cells }.toSet()
        val extraStones = (step.removed - inMatches).size
        val base = step.matches.sumOf { pointsForMatch(it.size) } + extraStones * POINTS_PER_SPECIAL_STONE
        (base * chainMultiplier(step.chain)).toInt()
    }
}
