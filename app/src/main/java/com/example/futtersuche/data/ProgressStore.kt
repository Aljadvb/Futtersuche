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
}
