package com.example.futtersuche.data

import android.content.Context

/**
 * Speichert den Fortschritt lokal auf dem Gerät (SharedPreferences = kleine Schlüssel/Wert-Datei).
 * Jede Kampagne hat ihren eigenen Fortschritt.
 */
class ProgressStore(context: Context) {

    private val prefs = context.getSharedPreferences("futtersuche_progress", Context.MODE_PRIVATE)

    /** Das Level, bei dem der Spieler in dieser Kampagne gerade steht (beginnt bei 1). */
    fun currentLevel(campaignId: String): Int = prefs.getInt("level_$campaignId", 1)

    /** Merkt sich, dass [level] erreicht wurde (nur wenn es höher ist als der bisherige Stand). */
    fun reachLevel(campaignId: String, level: Int) {
        if (level > currentLevel(campaignId)) {
            prefs.edit().putInt("level_$campaignId", level).apply()
        }
    }

    fun bestScore(campaignId: String, level: Int): Int = prefs.getInt("best_${campaignId}_$level", 0)

    /** Speichert [score], falls er ein neuer Bestwert ist. Gibt true zurück, wenn es ein neuer Rekord war. */
    fun saveBestScore(campaignId: String, level: Int, score: Int): Boolean {
        if (score <= bestScore(campaignId, level)) return false
        prefs.edit().putInt("best_${campaignId}_$level", score).apply()
        return true
    }

    /** Wurde der Hinweis mit diesem Schlüssel schon einmal gezeigt? */
    fun hintSeen(key: String): Boolean = prefs.getBoolean("hint_$key", false)

    fun markHintSeen(key: String) {
        prefs.edit().putBoolean("hint_$key", true).apply()
    }

    /** Speichert den laufenden Level-Stand (als Text, siehe SaveCodec). */
    fun saveGame(campaignId: String, text: String) {
        prefs.edit().putString("game_$campaignId", text).apply()
    }

    /** Der gespeicherte Level-Stand oder null, wenn keiner da ist. */
    fun loadGame(campaignId: String): String? = prefs.getString("game_$campaignId", null)

    /** Löscht den gespeicherten Level-Stand (z. B. wenn das Level vorbei ist). */
    fun clearGame(campaignId: String) {
        prefs.edit().remove("game_$campaignId").apply()
    }

    // ---------- Statistik für Achievements ----------

    fun addFoodCollected(campaignId: String, kind: Int, count: Int) = addInt("food_${campaignId}_$kind", count)

    fun recordChain(chain: Int) {
        if (chain > prefs.getInt("best_chain", 0)) prefs.edit().putInt("best_chain", chain).apply()
    }

    fun addWinWithFiveMovesLeft() = addInt("wins_5_left", 1)

    fun addIntrudersChased(count: Int) = addInt("intruders_chased", count)

    fun addObstaclesRemoved(count: Int) = addInt("obstacles_removed", count)

    private fun addInt(key: String, amount: Int) {
        if (amount != 0) prefs.edit().putInt(key, prefs.getInt(key, 0) + amount).apply()
    }

    /** Liest die komplette Statistik (für die Achievements). */
    fun stats(): PlayerStats = PlayerStats(
        levelReached = Campaigns.all.associate { it.id to currentLevel(it.id) },
        foodCollected = Campaigns.all.flatMap { campaign ->
            campaign.foods.indices.map { kind -> "${campaign.id}:$kind" to prefs.getInt("food_${campaign.id}_$kind", 0) }
        }.toMap(),
        bestChain = prefs.getInt("best_chain", 0),
        winsWithFiveMovesLeft = prefs.getInt("wins_5_left", 0),
        intrudersChased = prefs.getInt("intruders_chased", 0),
        obstaclesRemoved = prefs.getInt("obstacles_removed", 0),
    )

    // ---------- Achievements ----------

    /** IDs aller bereits erreichten Achievements. */
    fun reachedAchievements(): Set<String> =
        Achievements.all.map { it.id }.filter { prefs.getBoolean("achievement_$it", false) }.toSet()

    /**
     * Prüft alle Achievements gegen die aktuelle Statistik, speichert neu erreichte
     * und gibt sie zurück (für die Einblendung). Einmal erreicht bleibt erreicht.
     */
    fun unlockNewAchievements(): List<Achievement> {
        val stats = stats()
        val already = reachedAchievements()
        val new = Achievements.all.filter { it.id !in already && it.isReached(stats) }
        if (new.isNotEmpty()) {
            val editor = prefs.edit()
            for (achievement in new) editor.putBoolean("achievement_${achievement.id}", true)
            editor.apply()
        }
        return new
    }
}
