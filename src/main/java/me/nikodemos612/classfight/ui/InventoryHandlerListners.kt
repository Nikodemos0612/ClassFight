package me.nikodemos612.classfight.ui

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.plugin.Plugin

class InventoryHandlerListners(private val plugin: Plugin): Listener {

    private val handlers = listOf(
            ClasslessInventoryHandler(plugin),
            ShotgunnerInventoryHandler(plugin)
    )
    @EventHandler
    fun onPlayerInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked
        val slot = event.slot
        if (player is Player && event.clickedInventory != null && player.gameMode != GameMode.CREATIVE) {
            event.isCancelled = true


            getTeamName(from = player)?.let { safeTeamName ->
                for (handler in handlers) {
                    if (handler.canHandle(safeTeamName)) {
                        CharacterSelection(plugin).buildInventory(player, handler.skillSelectionInventory)
                    }
                }
            }

/*
            getTeamName(from = player)?.let { safeTeamName ->
                for (handler in handlers) {
                    if (handler.canHandle(safeTeamName) && handler.skillSelectionInventory[slot]?.skillCategory != "") {
                        handler.switchSkill(
                                player.uniqueId,
                                handler.skillSelectionInventory[slot]?.skillCategory.toString(),
                                handler.skillSelectionInventory[slot]?.skillName.toString()
                        )
                    }
                }
            }*/
        }
    }

    /**
     * Gets the name of the team from the Player
     * @param from the Player that could be on a team.
     * @return the name of the team of the player, if it has one.
     */
    private fun getTeamName(from: Player) =
            Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(from)?.name
}
