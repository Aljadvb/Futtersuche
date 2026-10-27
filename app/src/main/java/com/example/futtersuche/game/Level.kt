package com.example.futtersuche.game

import kotlin.math.min
import kotlin.random.Random

/** Ein Sammelziel: [amount] Stück der Futtersorte [kind]. */
data class Goal(val kind: Int, val amount: Int)

/**
 * Wie aktiv der Widersacher in einem Level ist.
 * Intervall = alle wie viele Züge er etwas tut; 0 = Verhalten in diesem Level aus.
 */
data class OpponentConfig(
    val startObstacles: Int = 0,
    val obstacleInterval: Int = 0,
    val thiefInterval: Int = 0,
    val intruderInterval: Int = 0,
) {
    val blockade: Boolean get() = startObstacles > 0 || obstacleInterval > 0
    val thief: Boolean get() = thiefInterval > 0
    val intruder: Boolean get() = intruderInterval > 0
}

/** Alles, was ein Level ausmacht. Wird aus der Levelnummer berechnet. */
data class LevelConfig(
    val number: Int,
    val moves: Int,
    val kindCount: Int,
    val goals: List<Goal>,
    val opponent: OpponentConfig = OpponentConfig(),
)

object LevelGenerator {

    /**
     * Erzeugt Level [level] für die Kampagne [campaignId].
     * Gleiche Nummer + gleiche Kampagne = immer dieselben Ziele (z. B. beim Wiederholen nach einer Niederlage).
     * Nur das Spielfeld wird bei jedem Versuch neu gemischt.
     */
    fun generate(level: Int, campaignId: String): LevelConfig {
        val random = Random(campaignId.hashCode() * 31 + level)
        val kindCount = Difficulty.kindCount(level)
        val amount = Difficulty.amountPerGoal(level)
        val goalKinds = (0 until kindCount).shuffled(random).take(Difficulty.goalCount(level))
        return LevelConfig(
            number = level,
            moves = Difficulty.moves(level),
            kindCount = kindCount,
            goals = goalKinds.map { Goal(it, amount) },
            opponent = OpponentConfig(
                startObstacles = Difficulty.startObstacles(level),
                obstacleInterval = Difficulty.obstacleInterval(level),
                thiefInterval = Difficulty.thiefInterval(level),
                intruderInterval = Difficulty.intruderInterval(level),
            ),
        )
    }
}

enum class LevelStatus { PLAYING, WON, LOST }

/** Der aktuelle Stand eines laufenden Levels. Unveränderlich – jeder Zug erzeugt einen neuen Stand. */
data class LevelState(
    val config: LevelConfig,
    val board: Board,
    val movesLeft: Int,
    val collected: Map<Int, Int> = emptyMap(),
    val score: Int = 0,
    /** Stein-ID des Futters, das der Futterdieb nach dem nächsten Zug klauen will (null = keins). */
    val thiefTargetId: Long? = null,
) {
    /** Wie viele Züge schon gemacht wurden. */
    val movesMade: Int get() = config.moves - movesLeft

    /** Gesammelte Menge für ein Ziel – höchstens so viel wie verlangt (für die Anzeige "7/12"). */
    fun progress(goal: Goal): Int = min(collected[goal.kind] ?: 0, goal.amount)

    val allGoalsReached: Boolean get() = config.goals.all { progress(it) >= it.amount }

    val status: LevelStatus
        get() = when {
            allGoalsReached -> LevelStatus.WON
            movesLeft <= 0 -> LevelStatus.LOST
            else -> LevelStatus.PLAYING
        }

    /** Bonuspunkte für übrige Züge (nur bei Sieg). */
    val leftoverBonus: Int get() = if (status == LevelStatus.WON) movesLeft * Scoring.POINTS_PER_LEFTOVER_MOVE else 0

    val finalScore: Int get() = score + leftoverBonus

    /** Neuer Stand nach einem erfolgreichen Zug. */
    fun afterMove(result: SwapResult.Success): LevelState {
        val newCollected = collected.toMutableMap()
        for ((kind, count) in result.collected) newCollected[kind] = (newCollected[kind] ?: 0) + count
        return copy(
            board = result.final,
            movesLeft = movesLeft - 1,
            collected = newCollected,
            score = score + Scoring.pointsFor(result),
        )
    }

    companion object {
        fun start(config: LevelConfig, random: Random = Random.Default): LevelState = LevelState(
            config = config,
            board = BoardGenerator.create(
                kindCount = config.kindCount,
                random = random,
                obstacles = BoardGenerator.randomObstacles(config.opponent.startObstacles, random = random),
            ),
            movesLeft = config.moves,
        )
    }
}
