package com.example.futtersuche.data

import androidx.compose.ui.graphics.Color

/**
 * Eine Futtersorte = eine Steinart auf dem Spielfeld.
 * [emoji] ist nur ein Platzhalter. Später kommt hier z. B. ein Bild (R.drawable.karotte) dazu.
 */
data class Food(
    val name: String,       // "Karotte"
    val pluralName: String, // "Karotten"
    val emoji: String,
)

/** Ein Tier oder ein Widersacher. */
data class Character(
    val name: String,    // "Hoppel"
    val species: String, // "Hase"
    val emoji: String,
    val article: String = "", // "der" / "die" – für Sätze wie "Der Fuchs klaut …"; leer bei Eigennamen
) {
    /** "der Fuchs" bzw. "Hoppel" */
    val nameWithArticle: String get() = if (article.isEmpty()) name else "$article $name"

    /** "Der Fuchs" – für den Satzanfang */
    val nameAtSentenceStart: String get() = nameWithArticle.replaceFirstChar { it.uppercase() }
}

/** Das Hindernis, das der Widersacher aufs Feld legt. */
data class Obstacle(
    val name: String,       // "Fuchsbau"
    val pluralName: String, // "Fuchsbauten"
    val emoji: String,
)

/**
 * Eine Kampagne – komplett als Daten beschrieben.
 * Neue Kampagne = einfach einen weiteren Eintrag in [Campaigns.all] hinzufügen.
 * (Freischalt-Bedingungen kommen in Schritt 6 dazu.)
 */
data class Campaign(
    val id: String,
    val title: String,
    val animal: Character,
    val opponent: Character,
    val obstacle: Obstacle,
    val foods: List<Food>, // mindestens 6 Sorten (ab Level 20 werden 6 benutzt)
    val backgroundLight: Color,
    val backgroundDark: Color,
)

object Campaigns {

    val hoppel = Campaign(
        id = "garten",
        title = "Hoppels Garten",
        animal = Character("Hoppel", "Hase", "🐰"),
        opponent = Character("Fuchs", "Fuchs", "🦊", article = "der"),
        obstacle = Obstacle("Fuchsbau", "Fuchsbauten", "🕳️"),
        foods = listOf(
            Food("Karotte", "Karotten", "🥕"),
            Food("Salat", "Salatköpfe", "🥬"),
            Food("Apfel", "Äpfel", "🍎"),
            Food("Radieschen", "Radieschen", "🍓"), // Platzhalter: es gibt kein Radieschen-Emoji
            Food("Erbse", "Erbsen", "🫛"),
            Food("Brokkoli", "Brokkoli", "🥦"),
        ),
        backgroundLight = Color(0xFFE8F5E9),
        backgroundDark = Color(0xFF1B2E1C),
    )

    val momo = Campaign(
        id = "dschungel",
        title = "Momos Dschungel",
        animal = Character("Momo", "Affe", "🐒"),
        opponent = Character("Schlange", "Schlange", "🐍", article = "die"),
        obstacle = Obstacle("Felsbrocken", "Felsbrocken", "🪨"),
        foods = listOf(
            Food("Banane", "Bananen", "🍌"),
            Food("Mango", "Mangos", "🥭"),
            Food("Kokosnuss", "Kokosnüsse", "🥥"),
            Food("Ananas", "Ananas", "🍍"),
            Food("Kiwi", "Kiwis", "🥝"),
            Food("Traube", "Trauben", "🍇"),
        ),
        backgroundLight = Color(0xFFFFF8E1),
        backgroundDark = Color(0xFF2E2614),
    )

    /** Alle Kampagnen in der Reihenfolge des Auswahlbildschirms. */
    val all: List<Campaign> = listOf(hoppel, momo)
}
