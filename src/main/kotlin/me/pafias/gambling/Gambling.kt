package me.pafias.gambling

import net.milkbowl.vault.economy.Economy
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class Gambling : JavaPlugin() {

    lateinit var plugin: Gambling
    lateinit var economy: Economy

    override fun onEnable() {
        plugin = this

        if (!setupEconomy()) logger.severe("No Vault or economy provider found!")

        getCommand("blackjack")!!.setExecutor(BlackjackCommand())
    }

    override fun onDisable() {
        server.onlinePlayers.forEach { obj: Player? -> obj!!.closeDialog() }
    }

    private fun setupEconomy(): Boolean {
        if (!server.pluginManager.isPluginEnabled("Vault")) return false

        val rsp = server.servicesManager.getRegistration<Economy?>(Economy::class.java) ?: return false

        economy = rsp.provider
        return true
    }

    fun get(): Gambling {
        return plugin
    }
}
