package com.example.futtersuche.ui

import com.example.futtersuche.data.Campaign
import com.example.futtersuche.game.CascadeStep
import com.example.futtersuche.game.OpponentConfig
import com.example.futtersuche.game.OpponentEvent

/** Die drei Verhaltensweisen des Widersachers – Schlüssel für "Hinweis schon gesehen". */
enum class OpponentBehavior(val key: String) { BLOCKADE("blockade"), THIEF("thief"), INTRUDER("intruder") }

fun OpponentConfig.behaviors(): List<OpponentBehavior> = buildList {
    if (blockade) add(OpponentBehavior.BLOCKADE)
    if (thief) add(OpponentBehavior.THIEF)
    if (intruder) add(OpponentBehavior.INTRUDER)
}

/** Erklärung beim ersten Auftauchen einer Verhaltensweise. */
fun hintTitle(campaign: Campaign, behavior: OpponentBehavior): String {
    val opp = campaign.opponent
    return when (behavior) {
        OpponentBehavior.BLOCKADE -> "${opp.emoji} ${opp.nameAtSentenceStart} legt ${campaign.obstacle.pluralName}!"
        OpponentBehavior.THIEF -> "${opp.emoji} ${opp.nameAtSentenceStart} ist ein Futterdieb!"
        OpponentBehavior.INTRUDER -> "${opp.emoji} ${opp.nameAtSentenceStart} schleicht sich aufs Feld!"
    }
}

fun hintText(campaign: Campaign, behavior: OpponentBehavior): String {
    val opp = campaign.opponent
    val obstacle = campaign.obstacle
    return when (behavior) {
        OpponentBehavior.BLOCKADE ->
            "${obstacle.emoji} ${obstacle.pluralName} versperren Felder. Man kann sie nicht bewegen.\n\n" +
                "So wirst du sie los: Mach einen Treffer direkt daneben!"
        OpponentBehavior.THIEF ->
            "Ab und zu markiert ${opp.nameWithArticle} ein Futter mit ${opp.emoji}. " +
                "Nach deinem nächsten Zug schnappt ${opp.nameWithArticle} es sich.\n\n" +
                "So rettest du es: Sammle das markierte Futter sofort ein!"
        OpponentBehavior.INTRUDER ->
            "${opp.nameAtSentenceStart} setzt sich auf ein Feld. Die Zahl zeigt, wie viele Züge noch Zeit bleibt. " +
                "Danach klaut ${opp.nameWithArticle} schon gesammeltes Futter oder legt ${obstacle.pluralName}.\n\n" +
                "So vertreibst du ${opp.nameWithArticle}: Mach einen Treffer direkt daneben!"
    }
}

/** Kurze Meldung, was der Spieler mit seinem Zug beim Widersacher erreicht hat. */
fun playerHitsText(campaign: Campaign, steps: List<CascadeStep>): String? {
    val parts = mutableListOf<String>()
    val removed = steps.sumOf { it.obstaclesRemoved.size }
    if (removed == 1) parts += "${campaign.obstacle.name} weg!"
    if (removed > 1) parts += "$removed ${campaign.obstacle.pluralName} weg!"
    if (steps.any { it.intruderChased }) parts += "${campaign.opponent.nameAtSentenceStart} ist vertrieben! 🎉"
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
}

/** Meldung zu den Aktionen des Widersachers. */
fun opponentEventText(campaign: Campaign, event: OpponentEvent): String {
    val opp = campaign.opponent
    val obstacle = campaign.obstacle
    return when (event) {
        is OpponentEvent.ThiefAnnounced ->
            "${opp.emoji} ${opp.nameAtSentenceStart} hat es auf ein Stück ${campaign.foods[event.kind].name} abgesehen!"
        is OpponentEvent.ThiefStole ->
            "${opp.emoji} ${opp.nameAtSentenceStart} hat ein Stück ${campaign.foods[event.kind].name} geklaut!"
        OpponentEvent.ThiefFoiled -> "Futter gerettet! ${opp.nameAtSentenceStart} geht leer aus."
        OpponentEvent.ObstaclePlaced -> "${obstacle.emoji} Ein neuer ${obstacle.name}!"
        OpponentEvent.IntruderAppeared -> "${opp.emoji} ${opp.nameAtSentenceStart} ist aufs Feld geschlichen!"
        is OpponentEvent.IntruderStole ->
            "${opp.emoji} ${opp.nameAtSentenceStart} hat ${event.amount} ${campaign.foods[event.kind].pluralName} stibitzt!"
        is OpponentEvent.IntruderBlocked ->
            "${opp.emoji} ${opp.nameAtSentenceStart} hat ${event.count} ${obstacle.pluralName} gelegt!"
    }
}
