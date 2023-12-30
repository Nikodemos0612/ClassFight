package me.nikodemos612.classfight.fighters

import me.nikodemos612.classfight.fighters.handlers.ShotgunnerFighterHandler
import me.nikodemos612.classfight.fighters.handlers.PotionDealerFighterHandler
import me.nikodemos612.classfight.fighters.handlers.SniperFighterHandler
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.entity.ThrowableProjectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.plugin.Plugin

class FighterHandlerListeners(plugin: Plugin): Listener {

    private val handlers = listOf(
        SniperFighterHandler(),
        PotionDealerFighterHandler(),
        ShotgunnerFighterHandler(plugin),
    )

    @EventHandler
    fun runItemHeldChangeHandler(event: PlayerItemHeldEvent) {
        val teamName = Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(event.player)?.name
        teamName?.let { safeTeamName ->
           for (handler in handlers) {
               if (handler.canHandle(safeTeamName))
                   handler.onItemHeldChange(event)
           }
        }
    }

    @EventHandler
    fun runPlayerInteractionHandler(event: PlayerInteractEvent) {
        val teamName = Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(event.player)?.name
        teamName?.let{ safeTeamName ->
            for (handler in handlers) {
                if (handler.canHandle(safeTeamName))
                    handler.onPlayerInteraction(event)
            }
        }
    }

    @EventHandler
    fun runProjectileHitHandler(event: ProjectileHitEvent) {
        if (event.entity is ThrowableProjectile) {
            val shooter = event.entity.shooter as? Player
            val teamName = shooter?.let { Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(it)?.name }
            teamName?.let { safeTeamName ->
                for (handler in handlers) {
                    if (handler.canHandle(safeTeamName))
                        handler.onProjectileHit(event)
                }
            }
        }
    }
}