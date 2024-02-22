package me.nikodemos612.classfight

import me.nikodemos612.classfight.fighters.FighterHandlerListeners
import me.nikodemos612.classfight.game.GameRulesHandler
import me.nikodemos612.classfight.ui.InventoryHandlerListners
import org.bukkit.entity.Player
import org.bukkit.inventory.PlayerInventory
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.collections.HashMap

@Suppress("unused")
class ClassFight : JavaPlugin() {
    override fun onEnable() {
        logger.info("CLASSFIGHT PLUGIN: the plugin is loading")

        // ===
        server.pluginManager.registerEvents(GameRulesHandler(), this)
        server.pluginManager.registerEvents(FighterHandlerListeners(this), this)
        server.pluginManager.registerEvents(InventoryHandlerListners(this), this)
        // ===

        logger.info("CLASSFIGHT PLUGIN: the plugin has loaded successfully!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
