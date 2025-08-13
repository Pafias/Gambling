package me.pafias.gambling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private final List<Card> cards = new ArrayList<>();

    public Deck() {
        // Create a standard 52 card deck
        for (Suit suit : Suit.values())
            for (Rank rank : Rank.values())
                cards.add(new Card(suit, rank));
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card deal() {
        if (cards.isEmpty())
            return null; // Should ideally reshuffle, but for one game this is fine.
        return cards.removeFirst();
    }
}