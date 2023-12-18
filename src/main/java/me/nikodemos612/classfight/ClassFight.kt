package me.nikodemos612.classfight

import me.nikodemos612.classfight.listeners.*
import org.bukkit.plugin.java.JavaPlugin

class ClassFight : JavaPlugin() {
    override fun onEnable() {
        server.pluginManager.registerEvents(MovementListener(), this)
        server.pluginManager.registerEvents(ItemHeldListener(), this)
        server.pluginManager.registerEvents(InventoryClickListener(), this)
        server.pluginManager.registerEvents(InteractionListener(), this)

        logger.info("The Plugin has loaded!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
