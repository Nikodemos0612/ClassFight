package me.nikodemos612.classfight

import me.nikodemos612.classfight.fighters.FighterHandlerListeners
import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class ClassFight : JavaPlugin() {

    override fun onEnable() {
        server.pluginManager.registerEvents(FighterHandlerListeners(this), this)

        logger.info("The Plugin has loaded!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
