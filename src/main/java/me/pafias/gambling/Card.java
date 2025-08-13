package me.pafias.gambling;

public record Card(Suit suit, Rank rank) {
    @Override
    public String toString() {
        // Formats the card for display, e.g., "[♥ K]"
        return "&f[" + suit.getIcon() + " " + rank.getDisplayName() + "&f]";
    }
}