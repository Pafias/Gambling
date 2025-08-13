package me.pafias.gambling

import io.papermc.paper.registry.RegistryBuilderFactory
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody
import net.milkbowl.vault.economy.Economy
import org.bukkit.entity.Player


class GameManager(private val player: Player) {

    private val economy: Economy?

    init {
        this.economy = Gambling.get().economy
    }

    /**
     * Shows the initial betting dialog to the player.
     */
    fun showBettingDialog() {
        player.showDialog(io.papermc.paper.dialog.Dialog.create(java.util.function.Consumer { builder: RegistryBuilderFactory<io.papermc.paper.dialog.Dialog?, out DialogRegistryEntry.Builder?>? ->
            builder.empty()
                .base(
                    DialogBase.builder(CC.a("&e&lBlackjack"))
                        .body(
                            java.util.List.of<PlainMessageDialogBody?>(
                                DialogBody.plainMessage(CC.EMPTY),
                                DialogBody.plainMessage(CC.a("&aWelcome to Blackjack!")),
                                DialogBody.plainMessage(CC.EMPTY),
                                DialogBody.plainMessage(CC.a("&7How much would you like to bet?")),
                                DialogBody.plainMessage(CC.EMPTY)
                            )
                        )
                        .build()
                )
                .type(
                    io.papermc.paper.registry.data.dialog.type.DialogType.multiAction(
                        java.util.List.of<ActionButton?>(
                            ActionButton.builder(CC.a("&a$10"))
                                .tooltip(CC.a("&7Click to bet $10"))
                                .action(
                                    DialogAction.staticAction(
                                        net.kyori.adventure.text.event.ClickEvent.callback(
                                            ClickCallback { audience: Audience? -> startGame(10.0) })
                                    )
                                )
                                .build(),
                            ActionButton.builder(CC.a("&e$50"))
                                .tooltip(CC.a("&7Click to bet $50"))
                                .action(
                                    DialogAction.staticAction(
                                        net.kyori.adventure.text.event.ClickEvent.callback(
                                            ClickCallback { audience: Audience? -> startGame(50.0) })
                                    )
                                )
                                .build(),
                            ActionButton.builder(CC.a("&6$100"))
                                .tooltip(CC.a("&7Click to bet $100"))
                                .action(
                                    DialogAction.staticAction(
                                        net.kyori.adventure.text.event.ClickEvent.callback(
                                            ClickCallback { audience: Audience? -> startGame(100.0) })
                                    )
                                )
                                .build(),
                            ActionButton.builder(CC.a("&c$500"))
                                .tooltip(CC.a("&7Click to bet $500"))
                                .action(
                                    DialogAction.staticAction(
                                        net.kyori.adventure.text.event.ClickEvent.callback(
                                            ClickCallback { audience: Audience? -> startGame(500.0) })
                                    )
                                )
                                .build(),
                            ActionButton.builder(CC.a("&7Close"))
                                .tooltip(CC.a("&7Click to chicken out"))
                                .action(
                                    DialogAction.staticAction(
                                        net.kyori.adventure.text.event.ClickEvent.callback(
                                            ClickCallback { obj: Audience? -> obj.closeDialog() })
                                    )
                                )
                                .build()
                        )
                    ).build()
                )
        }))
    }

    /**
     * Starts a new game after a bet is chosen.
     *
     * @param betAmount The amount the player is betting.
     */
    private fun startGame(betAmount: kotlin.Double) {
        if (economy != null && economy.getBalance(player) < betAmount) {
            player.sendMessage(CC.t("&cYou don't have enough money to place that bet!"))
            return
        }

        if (economy != null) economy.withdrawPlayer(player, betAmount)

        val game = BlackjackGame(betAmount)
        game.dealInitialHands()

        // Check for immediate blackjack
        if (game.playerHasBlackjack()) {
            if (game.dealerHasBlackjack()) {
                showResultDialog(game, BlackjackGame.GameResult.PUSH) // Both have blackjack
            } else {
                showResultDialog(game, BlackjackGame.GameResult.PLAYER_BLACKJACK) // Player has blackjack
            }
        } else {
            showGameDialog(game)
        }
    }

    /**
     * Shows the main game dialog (Hit or Stand).
     *
     * @param game The current game instance.
     */
    private fun showGameDialog(game: BlackjackGame) {
        val gameDialog =
            io.papermc.paper.dialog.Dialog.create(java.util.function.Consumer { builder: RegistryBuilderFactory<io.papermc.paper.dialog.Dialog?, out DialogRegistryEntry.Builder?>? ->
                builder.empty()
                    .base(
                        DialogBase.builder(CC.a("&e&lBlackjack"))
                            .body(
                                java.util.List.of<PlainMessageDialogBody?>(
                                    DialogBody.plainMessage(CC.a("")),
                                    DialogBody.plainMessage(CC.a("&eDealer's Hand: " + game.getDealerHandAsString(true))),
                                    DialogBody.plainMessage(CC.a("&aYour Hand: " + game.playerHandAsString + " &7(Value: " + game.playerHandValue + ")")),
                                    DialogBody.plainMessage(CC.a("")),
                                    DialogBody.plainMessage(CC.a("&7What do you want to do?"))
                                )
                            )
                            .build()
                    )
                    .type(
                        io.papermc.paper.registry.data.dialog.type.DialogType.multiAction(
                            java.util.List.of<ActionButton?>(
                                ActionButton.builder(CC.a("&aHit"))
                                    .tooltip(CC.a("&7Click to Hit (get another card)"))
                                    .action(
                                        DialogAction.staticAction(
                                            net.kyori.adventure.text.event.ClickEvent.callback(
                                                ClickCallback { audience: Audience? -> handleHit(game) })
                                        )
                                    )
                                    .build(),
                                ActionButton.builder(CC.a("&cStand"))
                                    .tooltip(CC.a("&7Click to Stand (keep your current hand)"))
                                    .action(
                                        DialogAction.staticAction(
                                            net.kyori.adventure.text.event.ClickEvent.callback(
                                                ClickCallback { audience: Audience? -> handleStand(game) })
                                        )
                                    )
                                    .build()
                            )
                        ).build()
                    )
            })
        player.showDialog(gameDialog)
    }

    /**
     * Handles the player choosing to "Hit".
     */
    private fun handleHit(game: BlackjackGame) {
        game.playerHit()
        if (game.isPlayerBust) {
            showResultDialog(game, BlackjackGame.GameResult.DEALER_WINS) // Player busts
        } else {
            showGameDialog(game) // Continue the game
        }
    }

    /**
     * Handles the player choosing to "Stand".
     */
    private fun handleStand(game: BlackjackGame) {
        val result: GameResult = game.dealerPlays()
        showResultDialog(game, result)
    }

    /**
     * Shows the final result of the game.
     *
     * @param game   The completed game instance.
     * @param result The outcome of the game.
     */
    private fun showResultDialog(game: BlackjackGame, result: GameResult) {
        val bet = game.bet
        var payout = 0.0
        val resultMessage: net.kyori.adventure.text.TextComponent

        when (result) {
            GameResult.PLAYER_WINS -> {
                payout = bet * 2
                if (economy != null) economy.depositPlayer(player, payout)
                resultMessage = CC.a("&a&lYOU WIN!")
                    .appendNewline()
                    .append(CC.a("&7You won &a$" + bet))
            }

            GameResult.PLAYER_BLACKJACK -> {
                payout = bet * 2.5 // Blackjack typically pays 3:2
                if (economy != null) economy.depositPlayer(player, payout)
                resultMessage = CC.a("&a&lBLACKJACK!")
                    .appendNewline()
                    .append(CC.a("&7You won &a$" + (payout - bet)))
            }

            GameResult.DEALER_WINS -> resultMessage = CC.a("&c&lYOU LOSE!")
                .appendNewline()
                .append(CC.a("&7You lost &c$" + bet))

            GameResult.PUSH -> {
                if (economy != null) economy.depositPlayer(player, bet) // Return the bet

                resultMessage = CC.a("&e&lPUSH!")
                    .appendNewline()
                    .append(CC.a("&7It's a tie. Bet returned."))
            }

            else -> resultMessage = CC.a("&cAn error occurred.")
        }

        player.showDialog(io.papermc.paper.dialog.Dialog.create(java.util.function.Consumer { builder: RegistryBuilderFactory<io.papermc.paper.dialog.Dialog?, out DialogRegistryEntry.Builder?>? ->
            builder.empty()
                .base(
                    DialogBase.builder(CC.a("&e&lBlackjack"))
                        .body(
                            java.util.List.of<PlainMessageDialogBody?>(
                                DialogBody.plainMessage(CC.EMPTY),
                                DialogBody.plainMessage(CC.a("&2&lGame Over")),
                                DialogBody.plainMessage(CC.EMPTY), DialogBody.plainMessage(CC.EMPTY),
                                DialogBody.plainMessage(CC.a("&eDealer's Hand: " + game.getDealerHandAsString(false) + " &7(" + game.dealerHandValue + ")")),
                                DialogBody.plainMessage(CC.EMPTY),
                                DialogBody.plainMessage(CC.a("&aYour Hand: " + game.playerHandAsString + " &7(" + game.playerHandValue + ")")),
                                DialogBody.plainMessage(CC.EMPTY), DialogBody.plainMessage(CC.EMPTY),
                                DialogBody.plainMessage(resultMessage),
                                DialogBody.plainMessage(CC.EMPTY)
                            )
                        )
                        .build()
                )
                .type(
                    io.papermc.paper.registry.data.dialog.type.DialogType.multiAction(
                        java.util.List.of<ActionButton?>(
                            ActionButton.builder(CC.a("&aPlay Again"))
                                .tooltip(CC.a("&7Click if you wish to play again"))
                                .action(
                                    DialogAction.staticAction(
                                        net.kyori.adventure.text.event.ClickEvent.callback(
                                            ClickCallback { audience: Audience? -> showBettingDialog() })
                                    )
                                )
                                .build(),
                            ActionButton.builder(CC.a("&7Close"))
                                .tooltip(CC.a("&7Click to quit the game"))
                                .action(
                                    DialogAction.staticAction(
                                        net.kyori.adventure.text.event.ClickEvent.callback(
                                            ClickCallback { obj: Audience? -> obj.closeDialog() })
                                    )
                                )
                                .build()
                        )
                    ).build()
                )
        }))
    }
}