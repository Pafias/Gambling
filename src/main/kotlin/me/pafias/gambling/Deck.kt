package me.pafias.gambling

import java.util.*

class Deck {
    private val cards: MutableList<Card?> = ArrayList<Card?>()

    init {
        // Create a standard 52 card deck
        for (suit in Suit.values()) for (rank in Rank.values()) cards.add(Card(suit, rank))
    }

    fun shuffle() {
        cards.shuffle()
    }

    fun deal(): Card? {
        if (cards.isEmpty()) return null // Should ideally reshuffle, but for one game this is fine.

        return cards.removeFirst()
    }
}