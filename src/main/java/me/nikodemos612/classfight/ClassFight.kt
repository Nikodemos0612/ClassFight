package me.nikodemos612.classfight

import me.nikodemos612.classfight.fighters.FighterHandlerListeners
import me.nikodemos612.classfight.game.GameRulesHandler
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.collections.HashMap

@Suppress("unused")
class ClassFight : JavaPlugin() {

    override fun onEnable() {
        logger.info("CLASSFIGHT PLUGIN: the plugin is loading")

        //private val projectileBounceCounter = HashMap<UUID, >()

        /*

        String fighterName

        vetor de skills

        // ==================================

        String fighterName
        vetor de inventário -> {
            iventario: {
              ItemStack
              skill
            }
        }

        // =============================

        skill -> Enum

        */

        server.pluginManager.registerEvents(GameRulesHandler(), this)
        server.pluginManager.registerEvents(FighterHandlerListeners(this), this)
        // ===

        logger.info("CLASSFIGHT PLUGIN: the plugin has loaded successfully!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}

enum class SniperSkills {
    SNIPER,
    SMALL_SNIPPER,
    WHATSAPP,
    HEAL,
}
