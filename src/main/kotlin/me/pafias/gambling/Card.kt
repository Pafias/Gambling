package me.pafias.gambling

data class Card(val suit: Suit?, val rank: Rank?) {
    override fun toString(): String {
        // Formats the card for display, e.g., "[♥ K]"
        return "&f[" + suit!!.icon + " " + rank!!.displayName + "&f]"
    }
}