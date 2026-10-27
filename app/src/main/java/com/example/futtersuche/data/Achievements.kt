package com.example.futtersuche.data

/**
 * Lebenslange Statistik des Spielers – daraus wird der Fortschritt aller Achievements berechnet.
 */
data class PlayerStats(
    /** Kampagnen-ID → erreichtes Level. */
    val levelReached: Map<String, Int> = emptyMap(),
    /** "Kampagnen-ID:Sorte" → insgesamt gesammelte Menge dieser Futtersorte. */
    val foodCollected: Map<String, Int> = emptyMap(),
    /** Längste Kettenreaktion (x2, x3, …) bisher. */
    val bestChain: Int = 0,
    /** Gewonnene Level mit 5 oder mehr übrigen Zügen. */
    val winsWithFiveMovesLeft: Int = 0,
    /** Wie oft der Eindringling vertrieben wurde. */
    val intrudersChased: Int = 0,
    /** Wie viele Hindernisse entfernt wurden. */
    val obstaclesRemoved: Int = 0,
)

/** Was ein Achievement misst. */
enum class Metric {
    /** Höchstes erreichtes Level in einer (beliebigen oder bestimmten) Kampagne. */
    LEVEL_REACHED,

    /** Meiste gesammelte Stück einer einzelnen Futtersorte. */
    FOOD_OF_ONE_KIND,

    /** Längste Kettenreaktion. */
    CHAIN,

    /** Gewonnene Level mit mindestens 5 übrigen Zügen. */
    WINS_WITH_FIVE_MOVES_LEFT,

    /** Eindringling vertrieben. */
    INTRUDERS_CHASED,

    /** Hindernisse entfernt. */
    OBSTACLES_REMOVED;

    /** Liest den aktuellen Wert aus der Statistik. [campaignId] = nur diese Kampagne zählen (null = alle). */
    fun valueIn(stats: PlayerStats, campaignId: String?): Int = when (this) {
        LEVEL_REACHED -> stats.levelReached.filterKeys { campaignId == null || it == campaignId }.values.maxOrNull() ?: 0
        FOOD_OF_ONE_KIND -> stats.foodCollected
            .filterKeys { campaignId == null || it.startsWith("$campaignId:") }.values.maxOrNull() ?: 0
        CHAIN -> stats.bestChain
        WINS_WITH_FIVE_MOVES_LEFT -> stats.winsWithFiveMovesLeft
        INTRUDERS_CHASED -> stats.intrudersChased
        OBSTACLES_REMOVED -> stats.obstaclesRemoved
    }
}

/**
 * Ein Achievement – komplett als Daten beschrieben.
 * Erreicht, sobald [metric] den Wert [target] erreicht.
 */
data class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val metric: Metric,
    val target: Int,
    /** Nur diese Kampagne zählt (null = egal welche). */
    val campaignId: String? = null,
) {
    /** Fortschritt für die Anzeige, höchstens [target]. */
    fun progress(stats: PlayerStats): Int = minOf(target, metric.valueIn(stats, campaignId))

    fun isReached(stats: PlayerStats): Boolean = progress(stats) >= target
}

/**
 * Alle Achievements. Neues Achievement = einfach einen Eintrag hinzufügen.
 * Die ID nie mehr ändern, sonst geht der gespeicherte Erfolg verloren.
 */
object Achievements {

    val all: List<Achievement> = listOf(
        Achievement(
            id = "level_10", emoji = "🏅", title = "Weit gekommen",
            description = "Erreiche Level 10 in einer Kampagne",
            metric = Metric.LEVEL_REACHED, target = 10,
        ),
        Achievement(
            id = "food_500", emoji = "🧺", title = "Volle Vorratskammer",
            description = "Sammle insgesamt 500 Stück einer Futtersorte",
            metric = Metric.FOOD_OF_ONE_KIND, target = 500,
        ),
        Achievement(
            id = "chain_4", emoji = "⚡", title = "Kettenreaktion!",
            description = "Schaffe eine Kettenreaktion x4",
            metric = Metric.CHAIN, target = 4,
        ),
        Achievement(
            id = "moves_left_5", emoji = "⏱️", title = "Flink wie ein Hase",
            description = "Gewinne ein Level mit 5 oder mehr übrigen Zügen",
            metric = Metric.WINS_WITH_FIVE_MOVES_LEFT, target = 1,
        ),
        Achievement(
            id = "chase_20", emoji = "🛡️", title = "Beschützer",
            description = "Vertreibe den Widersacher 20-mal",
            metric = Metric.INTRUDERS_CHASED, target = 20,
        ),
        Achievement(
            id = "obstacles_50", emoji = "⛏️", title = "Aufräumer",
            description = "Entferne 50 Hindernisse",
            metric = Metric.OBSTACLES_REMOVED, target = 50,
        ),
    )

    fun byId(id: String): Achievement? = all.firstOrNull { it.id == id }
}
