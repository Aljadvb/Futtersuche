package com.example.futtersuche.game

import com.example.futtersuche.data.Achievements
import com.example.futtersuche.data.Campaigns
import com.example.futtersuche.data.Metric
import com.example.futtersuche.data.PlayerStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementTest {

    private fun achievement(id: String) = Achievements.byId(id)!!

    @Test
    fun levelZehnInIrgendeinerKampagne() {
        val stats = PlayerStats(levelReached = mapOf("garten" to 9, "dschungel" to 10))
        assertTrue(achievement("level_10").isReached(stats))
        assertFalse(achievement("level_10").isReached(PlayerStats(levelReached = mapOf("garten" to 9))))
    }

    @Test
    fun futterZaehltProSorteNichtZusammen() {
        val stats = PlayerStats(foodCollected = mapOf("garten:0" to 300, "garten:1" to 300))
        assertFalse(achievement("food_500").isReached(stats)) // 600 insgesamt, aber keine Sorte hat 500
        assertEquals(300, achievement("food_500").progress(stats))
        assertTrue(achievement("food_500").isReached(PlayerStats(foodCollected = mapOf("dschungel:4" to 512))))
    }

    @Test
    fun fortschrittWirdBeimZielGedeckelt() {
        val stats = PlayerStats(obstaclesRemoved = 80)
        assertEquals(50, achievement("obstacles_50").progress(stats))
    }

    @Test
    fun metrikMitBestimmterKampagne() {
        val stats = PlayerStats(levelReached = mapOf("garten" to 15, "dschungel" to 3))
        assertEquals(3, Metric.LEVEL_REACHED.valueIn(stats, "dschungel"))
        assertEquals(15, Metric.LEVEL_REACHED.valueIn(stats, null))
    }

    @Test
    fun ersteKampagneOffenWeitereGesperrt() {
        assertTrue(Campaigns.all.first().isUnlocked(emptySet()))
        assertFalse(Campaigns.momo.isUnlocked(emptySet()))
        assertTrue(Campaigns.momo.isUnlocked(setOf(Campaigns.momo.unlockedBy!!)))
    }

    @Test
    fun alleVerweiseSindGueltig() {
        // Jede Freischalt-Bedingung muss auf ein existierendes Achievement zeigen
        for (campaign in Campaigns.all) {
            campaign.unlockedBy?.let { assertNotNull("${campaign.id} → $it", Achievements.byId(it)) }
            assertTrue("${campaign.id} braucht mindestens 6 Futtersorten", campaign.foods.size >= 6)
        }
        // IDs dürfen nicht doppelt vorkommen
        assertEquals(Achievements.all.size, Achievements.all.map { it.id }.toSet().size)
        assertEquals(Campaigns.all.size, Campaigns.all.map { it.id }.toSet().size)
    }
}
