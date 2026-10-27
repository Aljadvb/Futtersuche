package com.example.futtersuche.game

import kotlin.random.Random

/** Was der Widersacher in seinem Zug getan hat – für Meldungen auf dem Bildschirm. */
sealed interface OpponentEvent {
    /** Futterdieb hat sich ein Futter ausgesucht und schnappt nach dem nächsten Zug zu. */
    data class ThiefAnnounced(val kind: Int) : OpponentEvent

    /** Futterdieb hat sich [amount] Stück aus dem gesammelten Vorrat geschnappt. */
    data class ThiefStole(val kind: Int, val amount: Int) : OpponentEvent

    /** Futterdieb hatte nichts zum Klauen im Vorrat und hat stattdessen ein Hindernis gegraben. */
    data class ThiefDug(val kind: Int) : OpponentEvent

    /** Das markierte Futter wurde rechtzeitig eingesammelt. */
    data object ThiefFoiled : OpponentEvent

    /** Ein neues Hindernis liegt auf dem Feld. */
    data object ObstaclePlaced : OpponentEvent

    /** Der Eindringling ist aufgetaucht. */
    data object IntruderAppeared : OpponentEvent

    /** Der Eindringling hat bereits gesammeltes Futter geklaut. */
    data class IntruderStole(val kind: Int, val amount: Int) : OpponentEvent

    /** Der Eindringling hat Hindernisse gelegt. */
    data class IntruderBlocked(val count: Int) : OpponentEvent
}

/**
 * Ergebnis eines Widersacher-Zuges.
 * [afterActions] = Feld direkt nach seinen Aktionen (schon nachgerutscht),
 * [steps] = Kettenreaktionen, die dadurch zufällig entstanden sind,
 * [state] = neuer Level-Stand.
 */
data class OpponentTurn(
    val state: LevelState,
    val events: List<OpponentEvent>,
    val afterActions: Board,
    val steps: List<CascadeStep>,
    val reshuffled: Boolean,
)

object OpponentAI {

    /** Der Widersacher ist nach jedem Spielerzug dran (solange das Level noch läuft). */
    fun act(state: LevelState, random: Random): OpponentTurn {
        val config = state.config.opponent
        if (state.status != LevelStatus.PLAYING) {
            return OpponentTurn(state, emptyList(), state.board, emptyList(), false)
        }
        val moveNumber = state.movesMade
        val events = mutableListOf<OpponentEvent>()
        var board = state.board
        val collected = state.collected.toMutableMap()
        var thiefTarget = state.thiefTargetId

        // 1. Futterdieb: entweder zuschnappen (wenn schon etwas markiert ist) oder neu markieren.
        //    Wurde das markierte Futter nicht gerettet, klaut er aus dem gesammelten Vorrat –
        //    ist davon nichts da, gräbt er an der Stelle ein Hindernis.
        if (thiefTarget != null) {
            val pos = board.positions().firstOrNull { board[it]?.id == thiefTarget }
            if (pos != null) {
                val kind = board[pos]!!.kind
                val inStock = collected[kind] ?: 0
                val isGoal = state.config.goals.any { it.kind == kind }
                if (isGoal && inStock > 0) {
                    val amount = minOf(Difficulty.THIEF_STEALS, inStock)
                    collected[kind] = inStock - amount
                    events += OpponentEvent.ThiefStole(kind, amount)
                    board = board.removeAll(listOf(pos))
                } else if (board.obstacles.size < Difficulty.MAX_OBSTACLES_ON_BOARD) {
                    events += OpponentEvent.ThiefDug(kind)
                    board = board.withBlockers(obstacles = board.obstacles + pos)
                } else {
                    events += OpponentEvent.ThiefStole(kind, 0)
                    board = board.removeAll(listOf(pos))
                }
            } else {
                events += OpponentEvent.ThiefFoiled
            }
            thiefTarget = null
        } else if (config.thief && moveNumber % config.thiefInterval == 0) {
            pickThiefTarget(board, state, random)?.let { pos ->
                thiefTarget = board[pos]!!.id
                events += OpponentEvent.ThiefAnnounced(board[pos]!!.kind)
            }
        }

        // 2. Eindringling: warten, Schaden anrichten oder neu auftauchen
        val intruder = board.intruder
        if (intruder != null) {
            if (intruder.patience > 1) {
                board = board.withBlockers(intruder = intruder.copy(patience = intruder.patience - 1))
            } else {
                board = board.withBlockers(intruder = null) // er geht wieder – aber nicht ohne Schaden
                val stealable = state.config.goals.filter { (collected[it.kind] ?: 0) > 0 }
                if (stealable.isNotEmpty() && (random.nextBoolean() || freeTiles(board, thiefTarget).isEmpty())) {
                    val goal = stealable.maxBy { collected[it.kind] ?: 0 }
                    val amount = minOf(Difficulty.INTRUDER_STEALS, collected[goal.kind] ?: 0)
                    collected[goal.kind] = (collected[goal.kind] ?: 0) - amount
                    events += OpponentEvent.IntruderStole(goal.kind, amount)
                } else {
                    val placed = placeObstacles(board, Difficulty.INTRUDER_OBSTACLES, thiefTarget, random)
                    if (placed.obstacles.size > board.obstacles.size) {
                        events += OpponentEvent.IntruderBlocked(placed.obstacles.size - board.obstacles.size)
                    }
                    board = placed
                }
            }
        } else if (config.intruder && moveNumber % config.intruderInterval == 0) {
            freeTiles(board, thiefTarget).randomOrNull(random)?.let { pos ->
                board = board.withBlockers(intruder = Intruder(pos, Difficulty.INTRUDER_PATIENCE))
                events += OpponentEvent.IntruderAppeared
            }
        }

        // 3. Blockade: ab und zu ein neues Hindernis
        if (config.obstacleInterval > 0 && moveNumber % config.obstacleInterval == 0) {
            val placed = placeObstacles(board, 1, thiefTarget, random)
            if (placed.obstacles.size > board.obstacles.size) events += OpponentEvent.ObstaclePlaced
            board = placed
        }

        // 4. Lücken auffüllen; entstehen dabei zufällig Treffer, zählen sie für den Spieler
        val kindCount = state.config.kindCount
        val afterActions = Gravity.apply(board, kindCount, random)
        val steps = Cascade.resolve(afterActions, kindCount, random)
        for (step in steps) {
            for ((kind, count) in step.collected) collected[kind] = (collected[kind] ?: 0) + count
        }
        val afterCascade = steps.lastOrNull()?.afterFall ?: afterActions
        val reshuffled = !MoveFinder.hasPossibleMove(afterCascade)
        val final = if (reshuffled) MoveFinder.shuffle(afterCascade, kindCount, random) else afterCascade

        val newState = state.copy(
            board = final,
            collected = collected,
            score = state.score + Scoring.pointsFor(steps),
            thiefTargetId = thiefTarget,
        )
        return OpponentTurn(newState, events, afterActions, steps, reshuffled)
    }

    /** Bevorzugt Futter, das für ein noch nicht erfülltes Ziel gebraucht wird. */
    private fun pickThiefTarget(board: Board, state: LevelState, random: Random): Pos? {
        val wanted = state.config.goals.filter { state.progress(it) < it.amount }.map { it.kind }.toSet()
        val candidates = freeTiles(board, null)
        return candidates.filter { board[it]!!.kind in wanted }.randomOrNull(random)
            ?: candidates.randomOrNull(random)
    }

    /** Felder mit einem normalen Stein (kein Spezialstein, nicht vom Dieb markiert). */
    private fun freeTiles(board: Board, thiefTarget: Long?): List<Pos> = board.positions().filter {
        val tile = board[it]
        tile != null && !tile.isSpecial && tile.id != thiefTarget
    }

    /** Legt bis zu [count] Hindernisse auf zufällige normale Steine (der Stein verschwindet). */
    private fun placeObstacles(board: Board, count: Int, thiefTarget: Long?, random: Random): Board {
        val room = Difficulty.MAX_OBSTACLES_ON_BOARD - board.obstacles.size
        if (room <= 0) return board
        val spots = freeTiles(board, thiefTarget).shuffled(random).take(minOf(count, room))
        return board.withBlockers(obstacles = board.obstacles + spots)
    }
}
