package me.nikodemos612.classfight.fighters

import me.nikodemos612.classfight.fighters.handlers.SniperFighterHandler
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent

object FighterHandlerManager {
    fun runItemHeldChangeHandler(event: PlayerItemHeldEvent) {
        val teamName = Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(event.player)?.name
        teamName?.let {
            when {
                SniperFighterHandler.canHandle(teamName) -> SniperFighterHandler.onItemHeldChange(event)
            }
        }
    }

    fun runPlayerMovementHandler(event: PlayerMoveEvent) {
        val teamName = Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(event.player)?.name
        teamName?.let {
            when {
                SniperFighterHandler.canHandle(teamName) -> SniperFighterHandler.onPlayerMove(event)
            }
        }
    }

    fun runInventoryCLickHandler(event: InventoryClickEvent) {
        val teamName = Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(event.whoClicked as Player)?.name
        teamName?.let {
            when {
                SniperFighterHandler.canHandle(teamName) -> SniperFighterHandler.onInventoryClick(event)
            }
        }
    }

    fun runPlayerInteractionHandler(event: PlayerInteractEvent) {
        val teamName = Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(event.player)?.name
        teamName?.let{
            when {
                SniperFighterHandler.canHandle(teamName) -> SniperFighterHandler.onPlayerInteraction(event)
            }
        }
    }
}
