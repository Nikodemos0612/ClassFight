package me.nikodemos612.classfight

import me.nikodemos612.classfight.effects.DefaultPlayerEffectsHandler
import me.nikodemos612.classfight.fighters.FighterHandlerListeners
import me.nikodemos612.classfight.game.GameRulesHandler
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.collections.HashMap

@Suppress("unused")
class ClassFight : JavaPlugin() {

    override fun onEnable() {
        logger.info("CLASSFIGHT PLUGIN: the plugin is loading")

        val playerEffectsHandler = DefaultPlayerEffectsHandler(plugin = this)
        playerEffectsHandler.createEffectsTask()

        server.pluginManager.registerEvents(GameRulesHandler(playerEffectsHandler = playerEffectsHandler), this)
        server.pluginManager.registerEvents(
            FighterHandlerListeners(
                plugin = this,
                playerEffectsHandler = playerEffectsHandler
            ),
            this,
        )

        logger.info("CLASSFIGHT PLUGIN: the plugin has loaded successfully!")
    }

    override fun onDisable() {}
}