package me.nikodemos612.classfight

import me.nikodemos612.classfight.fighters.FighterHandlerListeners
import me.nikodemos612.classfight.listeners.*
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

class ClassFight : JavaPlugin() {

    override fun onEnable() {
        server.pluginManager.registerEvents(FighterHandlerListeners(this), this)

        logger.info("The Plugin has loaded!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
