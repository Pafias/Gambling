package me.pafias.gambling;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.pafias.putils.CC;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GameManager {

    private final Player player;

    @Nullable
    private final Economy economy;

    public GameManager(Player player) {
        this.player = player;
        this.economy = Gambling.getEconomy();
    }

    /**
     * Shows the initial betting dialog to the player.
     */
    public void showBettingDialog() {
        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(CC.a("&e&lBlackjack"))
                        .body(List.of(
                                DialogBody.plainMessage(CC.EMPTY),
                                DialogBody.plainMessage(CC.a("&aWelcome to Blackjack!")),
                                DialogBody.plainMessage(CC.EMPTY),
                                DialogBody.plainMessage(CC.a("&7How much would you like to bet?")),
                                DialogBody.plainMessage(CC.EMPTY)
                        ))
                        .build())
                .type(DialogType.multiAction(List.of(
                        ActionButton.builder(CC.a("&a$10"))
                                .tooltip(CC.a("&7&oClick to bet $10"))
                                .action(DialogAction.staticAction(ClickEvent.callback(audience -> startGame(10))))
                                .build(),
                        ActionButton.builder(CC.a("&e$50"))
                                .tooltip(CC.a("&7&oClick to bet $50"))
                                .action(DialogAction.staticAction(ClickEvent.callback(audience -> startGame(50))))
                                .build(),
                        ActionButton.builder(CC.a("&6$100"))
                                .tooltip(CC.a("&7&oClick to bet $100"))
                                .action(DialogAction.staticAction(ClickEvent.callback(audience -> startGame(100))))
                                .build(),
                        ActionButton.builder(CC.a("&c$500"))
                                .tooltip(CC.a("&7&oClick to bet $500"))
                                .action(DialogAction.staticAction(ClickEvent.callback(audience -> startGame(500))))
                                .build(),
                        ActionButton.builder(CC.a("&7Close"))
                                .tooltip(CC.a("&7&oClick to chicken out"))
                                .action(DialogAction.staticAction(ClickEvent.callback(Audience::closeDialog)))
                                .build()
                )).build())));
    }

    /**
     * Starts a new game after a bet is chosen.
     *
     * @param betAmount The amount the player is betting.
     */
    private void startGame(double betAmount) {
        if (economy != null && economy.getBalance(player) < betAmount) {
            player.sendMessage(CC.t("&cYou don't have enough money to place that bet!"));
            return;
        }

        if (economy != null)
            economy.withdrawPlayer(player, betAmount);

        BlackjackGame game = new BlackjackGame(betAmount);
        game.dealInitialHands();

        // Check for immediate blackjack
        if (game.playerHasBlackjack()) {
            if (game.dealerHasBlackjack()) {
                showResultDialog(game, BlackjackGame.GameResult.PUSH); // Both have blackjack
            } else {
                showResultDialog(game, BlackjackGame.GameResult.PLAYER_BLACKJACK); // Player has blackjack
            }
        } else {
            showGameDialog(game);
        }
    }

    /**
     * Shows the main game dialog (Hit or Stand).
     *
     * @param game The current game instance.
     */
    private void showGameDialog(BlackjackGame game) {
        Dialog gameDialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(CC.a("&e&lBlackjack"))
                        .body(List.of(
                                DialogBody.plainMessage(CC.a("")),
                                DialogBody.plainMessage(CC.a("&eDealer's Hand: " + game.getDealerHandAsString(true))),
                                DialogBody.plainMessage(CC.a("&aYour Hand: " + game.getPlayerHandAsString() + " &7(Value: " + game.getPlayerHandValue() + ")")),
                                DialogBody.plainMessage(CC.a("")),
                                DialogBody.plainMessage(CC.a("&7What do you want to do?"))
                        ))
                        .build())
                .type(DialogType.multiAction(List.of(
                        ActionButton.builder(CC.a("&aHit"))
                                .tooltip(CC.a("&7&oClick to Hit (get another card)"))
                                .action(DialogAction.staticAction(ClickEvent.callback(audience -> handleHit(game))))
                                .build(),
                        ActionButton.builder(CC.a("&cStand"))
                                .tooltip(CC.a("&7&oClick to Stand (keep your current hand)"))
                                .action(DialogAction.staticAction(ClickEvent.callback(audience -> handleStand(game))))
                                .build(),
                        ActionButton.builder(CC.a("&7Forfeit"))
                                .tooltip(CC.a("&7&oClick to forfeit your bet and end the game"))
                                .action(DialogAction.staticAction(ClickEvent.callback(audience -> handleForfeit(game))))
                                .build()
                )).build()));
        player.showDialog(gameDialog);
    }

    /**
     * Handles the player choosing to "Hit".
     */
    private void handleHit(BlackjackGame game) {
        game.playerHit();
        if (game.isPlayerBust()) {
            showResultDialog(game, BlackjackGame.GameResult.DEALER_WINS); // Player busts
        } else {
            showGameDialog(game); // Continue the game
        }
    }

    /**
     * Handles the player choosing to "Stand".
     */
    private void handleStand(BlackjackGame game) {
        BlackjackGame.GameResult result = game.dealerPlays();
        showResultDialog(game, result);
    }

    /**
     * Handles the player choosing to forfeit the game.
     */
    private void handleForfeit(BlackjackGame game) {
        showResultDialog(game, BlackjackGame.GameResult.FORFEIT);
    }

    /**
     * Shows the final result of the game.
     *
     * @param game   The completed game instance.
     * @param result The outcome of the game.
     */
    private void showResultDialog(BlackjackGame game, BlackjackGame.GameResult result) {
        double bet = game.getBet();
        double payout = 0;
        TextComponent resultMessage;

        switch (result) {
            case PLAYER_WINS:
                payout = bet * 2;
                if (economy != null)
                    economy.depositPlayer(player, payout);
                resultMessage = CC.a("&a&lYOU WIN!")
                        .appendNewline()
                        .append(CC.a("&7You won &a$" + bet));
                break;
            case PLAYER_BLACKJACK:
                payout = bet * 2.5; // Blackjack typically pays 3:2
                if (economy != null)
                    economy.depositPlayer(player, payout);
                resultMessage = CC.a("&a&lBLACKJACK!")
                        .appendNewline()
                        .append(CC.a("&7You won &a$" + (payout - bet)));
                break;
            case DEALER_WINS:
                resultMessage = CC.a("&c&lYOU LOSE!")
                        .appendNewline()
                        .append(CC.a("&7You lost &c$" + bet));
                break;
            case PUSH:
                if (economy != null)
                    economy.depositPlayer(player, bet); // Return the bet
                resultMessage = CC.a("&e&lPUSH!")
                        .appendNewline()
                        .append(CC.a("&7It's a tie. Bet returned."));
                break;
            case FORFEIT:
                resultMessage = CC.a("&a&lFORFEIT!")
                        .appendNewline()
                        .append(CC.a("&7You forfeited and lost your bet of &c$" + bet));
                break;
            default:
                resultMessage = CC.a("&cAn error occurred.");
                break;
        }

        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(CC.a("&e&lBlackjack"))
                        .body(List.of(
                                DialogBody.plainMessage(CC.EMPTY),
                                DialogBody.plainMessage(CC.a("&2&lGame Over")),
                                DialogBody.plainMessage(CC.EMPTY), DialogBody.plainMessage(CC.EMPTY),
                                DialogBody.plainMessage(CC.a("&eDealer's Hand: " + game.getDealerHandAsString(false) + " &7(" + game.getDealerHandValue() + ")")),
                                DialogBody.plainMessage(CC.EMPTY),
                                DialogBody.plainMessage(CC.a("&aYour Hand: " + game.getPlayerHandAsString() + " &7(" + game.getPlayerHandValue() + ")")),
                                DialogBody.plainMessage(CC.EMPTY), DialogBody.plainMessage(CC.EMPTY),
                                DialogBody.plainMessage(resultMessage),
                                DialogBody.plainMessage(CC.EMPTY)
                        ))
                        .build())
                .type(DialogType.multiAction(List.of(
                        ActionButton.builder(CC.a("&aPlay Again"))
                                .tooltip(CC.a("&7&oClick if you wish to play again"))
                                .action(DialogAction.staticAction(ClickEvent.callback(audience -> showBettingDialog())))
                                .build(),
                        ActionButton.builder(CC.a("&7Close"))
                                .tooltip(CC.a("&7&oClick to quit the game"))
                                .action(DialogAction.staticAction(ClickEvent.callback(Audience::closeDialog)))
                                .build()
                )).build())));
    }
}