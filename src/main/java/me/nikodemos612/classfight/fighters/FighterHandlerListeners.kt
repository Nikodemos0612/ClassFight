package me.nikodemos612.classfight.fighters

import me.nikodemos612.classfight.fighters.handlers.ShotgunnerFighterHandler
import me.nikodemos612.classfight.fighters.handlers.BangerFighterHandler
import me.nikodemos612.classfight.fighters.handlers.SniperFighterHandler
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.Plugin

class FighterHandlerListeners(plugin: Plugin): Listener {

    private val handlers = listOf(
        SniperFighterHandler(plugin),
        BangerFighterHandler(plugin),
        ShotgunnerFighterHandler(plugin),
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
}