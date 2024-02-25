package me.nikodemos612.classfight

import me.nikodemos612.classfight.fighters.FighterHandlerListeners
import me.nikodemos612.classfight.game.GameRulesHandler
import me.nikodemos612.classfight.ui.CharacterSelection
import me.nikodemos612.classfight.ui.InventoryHandlerListeners
import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class ClassFight : JavaPlugin() {

    override fun onEnable() {
        val characterSelection = CharacterSelection()

        logger.info("CLASSFIGHT PLUGIN: the plugin is loading")

        // ===
        server.pluginManager.registerEvents(GameRulesHandler(characterSelection), this)
        server.pluginManager.registerEvents(InventoryHandlerListeners(characterSelection), this)
        server.pluginManager.registerEvents(FighterHandlerListeners(this), this)
        // ===

        logger.info("CLASSFIGHT PLUGIN: the plugin has loaded successfully!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
