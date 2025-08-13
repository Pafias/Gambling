package me.pafias.gambling;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Gambling extends JavaPlugin {
    private static Gambling plugin;
    public static Gambling get() {
        return plugin;
    }

    private static Economy economy = null;
    public static Economy getEconomy() {
        return economy;
    }

    @Override
    public void onEnable() {
        plugin = this;

        if (!setupEconomy())
            getLogger().severe("No Vault or economy provider found!");

        getCommand("blackjack").setExecutor(new BlackjackCommand());
    }

    @Override
    public void onDisable() {
        getServer().getOnlinePlayers().forEach(Player::closeDialog);
    }

    private boolean setupEconomy() {
        if (!getServer().getPluginManager().isPluginEnabled("Vault"))
            return false;

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null)
            return false;

        economy = rsp.getProvider();
        return economy != null;
    }

}
