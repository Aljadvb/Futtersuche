package com.example.futtersuche.game

import kotlin.random.Random

/** Ergebnis eines Tausch-Versuchs. */
sealed interface SwapResult {
    /** Tausch gar nicht möglich (nicht benachbart, außerhalb oder leeres Feld). */
    data object NotAllowed : SwapResult

    /** Tausch möglich, ergibt aber keinen Treffer → Stein springt zurück. */
    data class NoMatch(val swapped: Board) : SwapResult

    /**
     * Tausch ergibt Treffer.
     * [swapped]  = Feld direkt nach dem Tausch.
     * [steps]    = alle Durchgänge inkl. Kettenreaktionen, in der Reihenfolge, in der sie passieren.
     * [final]    = Feld am Ende (schon neu gemischt, falls kein Zug mehr möglich war).
     * [reshuffled] = true, wenn am Ende neu gemischt werden musste.
     */
    data class Success(
        val swapped: Board,
        val steps: List<CascadeStep>,
        val final: Board,
        val reshuffled: Boolean,
    ) : SwapResult {
        /** Insgesamt gesammeltes Futter dieses Zuges: Sorte → Anzahl. */
        val collected: Map<Int, Int>
            get() = steps.flatMap { it.collected.entries }
                .groupingBy { it.key }
                .fold(0) { sum, entry -> sum + entry.value }

        /** Längste Kette dieses Zuges (1 = keine Kettenreaktion). */
        val maxChain: Int get() = steps.maxOfOrNull { it.chain } ?: 0
    }
}

object GameRules {

    fun trySwap(board: Board, a: Pos, b: Pos, kindCount: Int, random: Random): SwapResult {
        if (!board.isInside(a) || !board.isInside(b)) return SwapResult.NotAllowed
        if (!a.isNeighborOf(b)) return SwapResult.NotAllowed
        val tileA = board[a] ?: return SwapResult.NotAllowed
        val tileB = board[b] ?: return SwapResult.NotAllowed

        var swapped = board.swap(a, b)
        var forced = emptySet<Pos>()

        // Farbbombe: Tausch ist immer erlaubt und löscht die Sorte des Tauschpartners.
        val bombA = tileA.special == Special.COLOR_BOMB
        val bombB = tileB.special == Special.COLOR_BOMB
        if (bombA || bombB) {
            // Nach dem Tausch liegt tileA auf b und tileB auf a.
            forced = when {
                bombA && bombB -> swapped.positions().toSet() // zwei Bomben: ganzes Feld
                bombA -> SpecialEffects.allOfKind(swapped, tileB.kind).toSet() + b
                else -> SpecialEffects.allOfKind(swapped, tileA.kind).toSet() + a
            }
            // Die Bombe ist damit verbraucht und soll nicht zusätzlich ihre eigene Sorte löschen.
            val used = listOfNotNull(if (bombA) b else null, if (bombB) a else null)
            swapped = swapped.withTiles(used.associateWith { swapped[it]!!.copy(special = Special.NONE) })
        } else if (MatchFinder.find(swapped).isEmpty()) {
            return SwapResult.NoMatch(swapped)
        }

        val steps = Cascade.resolve(swapped, kindCount, random, forced, preferredSpecialSpots = setOf(a, b))
        val afterCascade = steps.lastOrNull()?.afterFall ?: swapped
        val needsShuffle = !MoveFinder.hasPossibleMove(afterCascade)
        val final = if (needsShuffle) MoveFinder.shuffle(afterCascade, kindCount, random) else afterCascade
        return SwapResult.Success(swapped, steps, final, needsShuffle)
    }
}
