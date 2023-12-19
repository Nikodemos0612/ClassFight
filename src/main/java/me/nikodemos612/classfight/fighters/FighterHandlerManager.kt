package me.nikodemos612.classfight.fighters

import me.nikodemos612.classfight.fighters.handlers.SniperFighterHandler
import me.nikodemos612.classfight.fighters.handlers.amogusus
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent

object FighterHandlerManager {
    fun runItemHeldChangeHandler(event: PlayerItemHeldEvent) {
        val teamName = Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(event.player)?.name
        teamName?.let {safeTeamName ->
            when {
                SniperFighterHandler.canHandle(safeTeamName) -> SniperFighterHandler.onItemHeldChange(event)
                amogusus.canHandle(safeTeamName) -> amogusus.onItemHeldChange(event)
            }
        }
    }

    fun runPlayerMovementHandler(event: PlayerMoveEvent) {
        val teamName = Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(event.player)?.name
        teamName?.let {safeTeamName ->
            when {
                SniperFighterHandler.canHandle(safeTeamName) -> SniperFighterHandler.onPlayerMove(event)
                amogusus.canHandle(safeTeamName) -> amogusus.onPlayerMove(event)
            }
        }
    }

    fun runInventoryCLickHandler(event: InventoryClickEvent) {
        val teamName = Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(event.whoClicked as Player)?.name
        teamName?.let {safeTeamName ->
            when {
                SniperFighterHandler.canHandle(safeTeamName) -> SniperFighterHandler.onInventoryClick(event)
                amogusus.canHandle(safeTeamName) -> amogusus.onInventoryClick(event)
            }
        }
    }

    fun runPlayerInteractionHandler(event: PlayerInteractEvent) {
        val teamName = Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(event.player)?.name
        teamName?.let{safeTeamName ->
            when {
                SniperFighterHandler.canHandle(safeTeamName) -> SniperFighterHandler.onPlayerInteraction(event)
                amogusus.canHandle(safeTeamName) -> amogusus.onPlayerInteraction(event)
            }
        }
    }
}