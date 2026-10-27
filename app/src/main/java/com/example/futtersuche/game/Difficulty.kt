package com.example.futtersuche.game

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * ALLE Stellschrauben für die Schwierigkeit an einem Ort.
 * Hier kannst du gefahrlos Zahlen ändern – der Rest des Spiels passt sich automatisch an.
 *
 * Tipp: Nach einer Änderung den Test "BalanceTest" laufen lassen (Rechtsklick → Run).
 * Er spielt die Level automatisch durch und zeigt, ob sie noch schaffbar sind.
 */
object Difficulty {

    // ---------- Züge ----------
    /** Züge in Level 1. */
    const val START_MOVES = 25

    /** Weniger Züge gibt es nie. */
    const val MIN_MOVES = 15

    /** Alle so viele Level gibt es einen Zug weniger. */
    const val LEVELS_PER_MOVE_LESS = 3

    // ---------- Sammelziele ----------
    /** Ab diesem Level wünscht sich das Tier zwei Futtersorten. */
    const val TWO_GOALS_FROM_LEVEL = 6

    /** Ab diesem Level wünscht sich das Tier drei Futtersorten. */
    const val THREE_GOALS_FROM_LEVEL = 15

    /** Menge pro Ziel in Level 1. */
    const val START_AMOUNT = 10

    /** So viel mehr Stück pro Ziel kommt mit jedem Level dazu (0.3 = etwa alle 3 Level eins mehr). */
    const val AMOUNT_GROWTH_PER_LEVEL = 0.3

    // ---------- Steinarten ----------
    /** Steinarten am Anfang. */
    const val START_KINDS = 5

    /** Ab diesem Level gibt es eine Steinart mehr (6 statt 5) – das macht Treffer seltener. */
    const val SIX_KINDS_FROM_LEVEL = 20

    // ---------- Schaffbarkeit ----------
    /**
     * So viel Ziel-Futter sammelt man typischerweise pro Zug (bei 5 Steinarten),
     * abhängig von der Anzahl der Ziele. Mehr Ziele = öfter passt ein Treffer zu einem Ziel.
     * Die Werte sind bewusst vorsichtig (ca. 60 % dessen, was der Computer-Spieler im
     * BalanceTest schafft), damit auch Kinder die Level gewinnen können.
     */
    val COLLECT_PER_MOVE = mapOf(1 to 1.4, 2 to 2.0, 3 to 2.1)

    // ---------- Berechnungen (normalerweise nicht ändern) ----------

    fun moves(level: Int): Int = max(MIN_MOVES, START_MOVES - (level - 1) / LEVELS_PER_MOVE_LESS)

    fun goalCount(level: Int): Int = when {
        level >= THREE_GOALS_FROM_LEVEL -> 3
        level >= TWO_GOALS_FROM_LEVEL -> 2
        else -> 1
    }

    fun kindCount(level: Int): Int = if (level >= SIX_KINDS_FROM_LEVEL) START_KINDS + 1 else START_KINDS

    /** Gewünschte Menge pro Ziel – aber nie mehr, als mit den Zügen typischerweise machbar ist. */
    fun amountPerGoal(level: Int): Int {
        val wanted = (START_AMOUNT + AMOUNT_GROWTH_PER_LEVEL * (level - 1)).roundToInt()
        return minOf(wanted, maxAmountPerGoal(level))
    }

    /** Obergrenze pro Ziel, damit das Level realistisch schaffbar bleibt. */
    fun maxAmountPerGoal(level: Int): Int {
        val goals = goalCount(level)
        val rate = COLLECT_PER_MOVE.getValue(goals) * START_KINDS / kindCount(level)
        return floor(moves(level) * rate / goals).toInt()
    }
}
