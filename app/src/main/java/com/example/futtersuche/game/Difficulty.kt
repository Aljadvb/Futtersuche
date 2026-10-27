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

    // ---------- Widersacher: Blockade ----------
    /** Ab diesem Level legt der Widersacher Hindernisse. */
    const val BLOCKADE_FROM_LEVEL = 5

    /** Hindernisse beim Levelstart im ersten Blockade-Level … */
    const val START_OBSTACLES = 2

    /** … alle so viele Level kommt eins dazu … */
    const val LEVELS_PER_EXTRA_OBSTACLE = 5

    /** … aber nie mehr als so viele beim Start. */
    const val MAX_START_OBSTACLES = 6

    /** Ab diesem Level legt er auch WÄHREND des Spiels neue Hindernisse. */
    const val OBSTACLE_DURING_GAME_FROM_LEVEL = 10

    /** Alle wie viele Züge er ein neues Hindernis legt: Startwert und Minimum. */
    const val OBSTACLE_INTERVAL_START = 7
    const val OBSTACLE_INTERVAL_MIN = 4

    /** Mehr Hindernisse als das liegen nie gleichzeitig auf dem Feld. */
    const val MAX_OBSTACLES_ON_BOARD = 10

    // ---------- Widersacher: Futterdieb ----------
    /** Ab diesem Level klaut der Widersacher Futter vom Feld. */
    const val THIEF_FROM_LEVEL = 12

    /** Alle wie viele Züge er klaut: Startwert und Minimum. */
    const val THIEF_INTERVAL_START = 6
    const val THIEF_INTERVAL_MIN = 3

    /**
     * Wird das markierte Futter nicht gerettet, klaut er so viele Stück aus dem
     * gesammelten Vorrat. Ist von der Sorte noch nichts gesammelt, gräbt er ein Hindernis.
     */
    const val THIEF_STEALS = 2

    // ---------- Widersacher: Eindringling ----------
    /** Ab diesem Level erscheint der Widersacher selbst auf dem Feld. */
    const val INTRUDER_FROM_LEVEL = 20

    /** Alle wie viele Züge er auftaucht (wenn er nicht schon da ist): Startwert und Minimum. */
    const val INTRUDER_INTERVAL_START = 9
    const val INTRUDER_INTERVAL_MIN = 5

    /** So viele Züge wartet er, bevor er Schaden anrichtet. */
    const val INTRUDER_PATIENCE = 3

    /** Schaden: so viel bereits gesammeltes Futter klaut er … */
    const val INTRUDER_STEALS = 3

    /** … oder so viele Hindernisse legt er. */
    const val INTRUDER_OBSTACLES = 2

    /** Alle so viele Level werden die Abstände der Widersacher-Aktionen um 1 Zug kürzer. */
    const val LEVELS_PER_FASTER_OPPONENT = 6

    // ---------- Schaffbarkeit ----------
    /**
     * So viel Ziel-Futter sammelt man typischerweise pro Zug (bei 5 Steinarten),
     * abhängig von der Anzahl der Ziele. Mehr Ziele = öfter passt ein Treffer zu einem Ziel.
     * Die Werte sind bewusst vorsichtig (ca. 60 % dessen, was der Computer-Spieler im
     * BalanceTest schafft), damit auch Kinder die Level gewinnen können.
     */
    val COLLECT_PER_MOVE = mapOf(1 to 1.4, 2 to 2.0, 3 to 2.1)

    /**
     * Der Widersacher kostet Züge (Hindernisse wegräumen, Eindringling vertreiben).
     * Pro aktiver Verhaltensweise wird die machbare Menge mit diesem Faktor verringert.
     */
    const val BLOCKADE_FACTOR = 1.0
    const val THIEF_FACTOR = 0.9
    const val INTRUDER_FACTOR = 0.95

    // ---------- Berechnungen (normalerweise nicht ändern) ----------

    fun moves(level: Int): Int = max(MIN_MOVES, START_MOVES - (level - 1) / LEVELS_PER_MOVE_LESS)

    fun goalCount(level: Int): Int = when {
        level >= THREE_GOALS_FROM_LEVEL -> 3
        level >= TWO_GOALS_FROM_LEVEL -> 2
        else -> 1
    }

    fun startObstacles(level: Int): Int =
        if (level < BLOCKADE_FROM_LEVEL) 0
        else minOf(MAX_START_OBSTACLES, START_OBSTACLES + (level - BLOCKADE_FROM_LEVEL) / LEVELS_PER_EXTRA_OBSTACLE)

    /** Abstand in Zügen für eine Widersacher-Aktion, die ab [fromLevel] aktiv ist. 0 = noch nicht aktiv. */
    private fun interval(level: Int, fromLevel: Int, start: Int, min: Int): Int =
        if (level < fromLevel) 0 else max(min, start - (level - fromLevel) / LEVELS_PER_FASTER_OPPONENT)

    fun obstacleInterval(level: Int): Int =
        interval(level, OBSTACLE_DURING_GAME_FROM_LEVEL, OBSTACLE_INTERVAL_START, OBSTACLE_INTERVAL_MIN)

    fun thiefInterval(level: Int): Int = interval(level, THIEF_FROM_LEVEL, THIEF_INTERVAL_START, THIEF_INTERVAL_MIN)

    fun intruderInterval(level: Int): Int =
        interval(level, INTRUDER_FROM_LEVEL, INTRUDER_INTERVAL_START, INTRUDER_INTERVAL_MIN)

    fun kindCount(level: Int): Int = if (level >= SIX_KINDS_FROM_LEVEL) START_KINDS + 1 else START_KINDS

    /**
     * Gewünschte Menge pro Ziel – aber nie mehr, als mit den Zügen typischerweise machbar ist.
     *
     * Damit es keinen Sprung gibt (z. B. wenn ab Level 20 eine Steinart dazukommt),
     * schauen wir ein paar Level voraus: Die Menge darf von Level zu Level höchstens
     * um 1 sinken. Muss sie später deutlich tiefer liegen, sinkt sie schon vorher langsam ab.
     */
    fun amountPerGoal(level: Int): Int {
        var amount = rawAmountPerGoal(level)
        for (ahead in 1..LOOKAHEAD_LEVELS) {
            if (goalCount(level + ahead) != goalCount(level)) break
            amount = minOf(amount, rawAmountPerGoal(level + ahead) + ahead)
        }
        return amount
    }

    private const val LOOKAHEAD_LEVELS = 10

    private fun rawAmountPerGoal(level: Int): Int {
        val wanted = (START_AMOUNT + AMOUNT_GROWTH_PER_LEVEL * (level - 1)).roundToInt()
        return minOf(wanted, maxAmountPerGoal(level))
    }

    /** Obergrenze pro Ziel, damit das Level realistisch schaffbar bleibt. */
    fun maxAmountPerGoal(level: Int): Int {
        val goals = goalCount(level)
        var rate = COLLECT_PER_MOVE.getValue(goals) * START_KINDS / kindCount(level)
        if (level >= BLOCKADE_FROM_LEVEL) rate *= BLOCKADE_FACTOR
        if (level >= THIEF_FROM_LEVEL) rate *= THIEF_FACTOR
        if (level >= INTRUDER_FROM_LEVEL) rate *= INTRUDER_FACTOR
        return floor(moves(level) * rate / goals).toInt()
    }
}
