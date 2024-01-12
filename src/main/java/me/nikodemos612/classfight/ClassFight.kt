package me.nikodemos612.classfight

import me.nikodemos612.classfight.fighters.FighterHandlerListeners
import me.nikodemos612.classfight.fighters.handlers.FangsPublicArgs
import me.nikodemos612.classfight.game.GameRulesHandler
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.math.roundToInt

@Suppress("unused")
class ClassFight : JavaPlugin() {

    override fun onEnable() {
        logger.info("CLASSFIGHT PLUGIN: the plugin is loading")

        server.pluginManager.registerEvents(GameRulesHandler(), this)
        server.pluginManager.registerEvents(FighterHandlerListeners(this), this)

        logger.info("CLASSFIGHT PLUGIN: the plugin has loaded successfully!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
