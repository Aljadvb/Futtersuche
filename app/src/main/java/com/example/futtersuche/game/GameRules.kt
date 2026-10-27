package com.example.futtersuche.game

/** Ergebnis eines Tausch-Versuchs. */
sealed interface SwapResult {
    /** Tausch gar nicht möglich (nicht benachbart, außerhalb oder leeres Feld). */
    data object NotAllowed : SwapResult

    /** Tausch möglich, ergibt aber keinen Treffer → Stein springt zurück. */
    data class NoMatch(val swapped: Board) : SwapResult

    /**
     * Tausch ergibt Treffer.
     * [swapped] = Feld direkt nach dem Tausch, [cleared] = Feld nach dem Entfernen der Treffer.
     */
    data class Success(
        val swapped: Board,
        val matches: List<Match>,
        val removed: Set<Pos>,
        val cleared: Board,
    ) : SwapResult
}

object GameRules {

    fun trySwap(board: Board, a: Pos, b: Pos): SwapResult {
        if (!board.isInside(a) || !board.isInside(b)) return SwapResult.NotAllowed
        if (!a.isNeighborOf(b)) return SwapResult.NotAllowed
        if (board[a] == null || board[b] == null) return SwapResult.NotAllowed

        val swapped = board.swap(a, b)
        val matches = MatchFinder.find(swapped)
        if (matches.isEmpty()) return SwapResult.NoMatch(swapped)

        // Ein Stein kann in zwei Treffern gleichzeitig stecken (z. B. bei L- oder T-Form),
        // deshalb sammeln wir die Positionen in einem Set – so zählt jeder Stein nur einmal.
        val removed = matches.flatMap { it.cells }.toSet()
        return SwapResult.Success(swapped, matches, removed, swapped.removeAll(removed))
    }
}
