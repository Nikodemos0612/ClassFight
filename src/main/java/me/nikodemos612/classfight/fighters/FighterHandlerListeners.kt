package me.nikodemos612.classfight.fighters

import me.nikodemos612.classfight.fighters.handlers.Amogusus
import me.nikodemos612.classfight.fighters.handlers.BangerFighterHandler
import me.nikodemos612.classfight.fighters.handlers.PotionDealerFighterHandler
import me.nikodemos612.classfight.fighters.handlers.SniperFighterHandler
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.entity.ThrowableProjectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.Plugin

class FighterHandlerListeners(plugin: Plugin): Listener {

    private val handlers = listOf(
        SniperFighterHandler(plugin),
        BangerFighterHandler(plugin),
        Amogusus(plugin),
        PotionDealerFighterHandler(),
    )

    @EventHandler
    fun runItemHeldChangeHandler(event: PlayerItemHeldEvent) {
        val teamName = Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(event.player)?.name
        teamName?.let {
           for (handler in handlers) {
               if (handler.canHandle(teamName))
                   handler.onItemHeldChange(event)
           }
        }
    }

    @EventHandler
    fun runPlayerMovementHandler(event: PlayerMoveEvent) {
        val teamName = Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(event.player)?.name
        teamName?.let {
            for (handler in handlers) {
                if (handler.canHandle(teamName))
                    handler.onPlayerMove(event)
            }
        }
    }

    @EventHandler
    fun runInventoryCLickHandler(event: InventoryClickEvent) {
        val teamName = Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(event.whoClicked as Player)?.name
        teamName?.let {
            for (handler in handlers) {
                if (handler.canHandle(teamName))
                    handler.onInventoryClick(event)
            }
        }
    }

    @EventHandler
    fun runPlayerInteractionHandler(event: PlayerInteractEvent) {
        val teamName = Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(event.player)?.name
        teamName?.let{
            for (handler in handlers) {
                if (handler.canHandle(teamName))
                    handler.onPlayerInteraction(event)
            }
        }
    }

    @EventHandler
    fun runProjectileHitHandler(event: ProjectileHitEvent) {
        if (event.entity is ThrowableProjectile) {
            val shooter = event.entity.shooter as? Player
            val teamName = shooter?.let { Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(it)?.name }
            teamName?.let {
                for (handler in handlers) {
                    if (handler.canHandle(teamName))
                        handler.onProjectileHit(event)
                }
            }
        }
    }
}