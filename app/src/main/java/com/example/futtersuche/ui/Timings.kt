package com.example.futtersuche.ui

/**
 * Alle Animationsdauern an einem Ort (Millisekunden).
 * Der GameController wartet genau so lange, wie die Animationen auf dem Spielfeld dauern.
 */
object Timings {
    /** Zwei Steine tauschen ihre Plätze. */
    const val SWAP_MS = 180

    /** Getroffene Steine leuchten auf und schrumpfen weg. */
    const val VANISH_MS = 280

    /** Kurze Pause, in der die Lücken zu sehen sind. */
    const val GAP_MS = 60

    /** Steine fallen nach (längster möglicher Fall). */
    const val FALL_MS = 420

    /** Futter fliegt zum Tier. */
    const val FLIGHT_MS = 700

    /** Kurze Pause, bevor der Widersacher reagiert. */
    const val OPPONENT_MS = 350

    /** So lange zeigt das Tier/der Widersacher eine Reaktion. */
    const val MOOD_MS = 1500L
}
