package me.pafias.gambling

import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.collections.MutableList

class BlackjackGame(val bet: Double) {
    enum class GameResult {
        PLAYER_WINS, DEALER_WINS, PUSH, PLAYER_BLACKJACK
    }

    private val playerHand: MutableList<Card> = ArrayList<Card>()
    private val dealerHand: MutableList<Card> = ArrayList<Card>()
    private val deck: Deck = Deck()

    init {
        this.deck.shuffle()
    }

    fun dealInitialHands() {
        deck.deal()?.let { playerHand.add(it) }
        deck.deal()?.let { dealerHand.add(it) }
        deck.deal()?.let { playerHand.add(it) }
        deck.deal()?.let { dealerHand.add(it) }
    }

    fun playerHit() {
        deck.deal()?.let { playerHand.add(it) }
    }

    fun dealerPlays(): GameResult {
        // Dealer must hit until their hand is 17 or more
        while (getHandValue(dealerHand) < 17) {
            deck.deal()?.let { dealerHand.add(it) }
        }

        val playerValue = this.playerHandValue
        val dealerValue = this.dealerHandValue

        if (dealerValue > 21) {
            return GameResult.PLAYER_WINS // Dealer busts
        } else if (playerValue > dealerValue) {
            return GameResult.PLAYER_WINS // Player wins
        } else if (dealerValue > playerValue) {
            return GameResult.DEALER_WINS // Dealer wins
        } else {
            return GameResult.PUSH // Tie
        }
    }

    val playerHandValue: Int
        get() = getHandValue(playerHand)

    val dealerHandValue: Int
        get() = getHandValue(dealerHand)

    val isPlayerBust: Boolean
        get() = this.playerHandValue > 21

    fun playerHasBlackjack(): Boolean {
        return this.playerHandValue == 21 && playerHand.size == 2
    }

    fun dealerHasBlackjack(): Boolean {
        return this.dealerHandValue == 21 && dealerHand.size == 2
    }

    val playerHandAsString: String
        get() = handToString(playerHand)

    fun getDealerHandAsString(hideFirstCard: Boolean): String? {
        if (hideFirstCard) {
            return "&f[??]" + " " + dealerHand.get(1).toString()
        }
        return handToString(dealerHand)
    }

    private fun handToString(hand: MutableList<Card>): String {
        return hand.stream().map<String?> { obj: Card? -> obj.toString() }.collect(Collectors.joining(" "))
    }

    private fun getHandValue(hand: MutableList<Card>): Int {
        var value = 0
        var aceCount = 0

        for (card in hand) {
            value += card.rank.value
            if (card.rank == Rank.ACE) aceCount++
        }

        // Adjust for Aces. If the total value is over 21, treat Aces as 1 instead of 11.
        while (value > 21 && aceCount > 0) {
            value -= 10
            aceCount--
        }

        return value
    }
}