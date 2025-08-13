package me.pafias.gambling;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BlackjackGame {

    public enum GameResult {
        PLAYER_WINS, DEALER_WINS, PUSH, PLAYER_BLACKJACK, FORFEIT
    }

    private final List<Card> playerHand = new ArrayList<>();
    private final List<Card> dealerHand = new ArrayList<>();
    private final Deck deck;
    private final double bet;

    public BlackjackGame(double bet) {
        this.deck = new Deck();
        this.bet = bet;
        this.deck.shuffle();
    }

    public void dealInitialHands() {
        playerHand.add(deck.deal());
        dealerHand.add(deck.deal());
        playerHand.add(deck.deal());
        dealerHand.add(deck.deal());
    }

    public void playerHit() {
        playerHand.add(deck.deal());
    }

    public GameResult dealerPlays() {
        // Dealer must hit until their hand is 17 or more
        while (getHandValue(dealerHand) < 17) {
            dealerHand.add(deck.deal());
        }

        int playerValue = getPlayerHandValue();
        int dealerValue = getDealerHandValue();

        if (dealerValue > 21) {
            return GameResult.PLAYER_WINS; // Dealer busts
        } else if (playerValue > dealerValue) {
            return GameResult.PLAYER_WINS; // Player wins
        } else if (dealerValue > playerValue) {
            return GameResult.DEALER_WINS; // Dealer wins
        } else {
            return GameResult.PUSH; // Tie
        }
    }

    public int getPlayerHandValue() {
        return getHandValue(playerHand);
    }

    public int getDealerHandValue() {
        return getHandValue(dealerHand);
    }

    public double getBet() {
        return bet;
    }

    public boolean isPlayerBust() {
        return getPlayerHandValue() > 21;
    }

    public boolean playerHasBlackjack() {
        return getPlayerHandValue() == 21 && playerHand.size() == 2;
    }

    public boolean dealerHasBlackjack() {
        return getDealerHandValue() == 21 && dealerHand.size() == 2;
    }

    public String getPlayerHandAsString() {
        return handToString(playerHand);
    }

    public String getDealerHandAsString(boolean hideFirstCard) {
        if (hideFirstCard) {
            return "&f[??]" + " " + dealerHand.get(1).toString();
        }
        return handToString(dealerHand);
    }

    private String handToString(List<Card> hand) {
        return hand.stream().map(Card::toString).collect(Collectors.joining(" "));
    }

    private int getHandValue(List<Card> hand) {
        int value = 0;
        int aceCount = 0;

        for (Card card : hand) {
            value += card.rank().getValue();
            if (card.rank() == Rank.ACE)
                aceCount++;
        }

        // Adjust for Aces. If the total value is over 21, treat Aces as 1 instead of 11.
        while (value > 21 && aceCount > 0) {
            value -= 10;
            aceCount--;
        }

        return value;
    }
}