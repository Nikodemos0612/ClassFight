package me.nikodemos612.classfight.ui

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class InventoryHandlerListeners(private val characterSelection: CharacterSelection): Listener {

    private val handlers: List<DefaultInventoryHandler> = listOf(
            ShotgunnerInventoryHandler()
    )

    @EventHandler
    fun onPlayerInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked
        val slot = event.slot
        if (player is Player && event.clickedInventory != null && player.gameMode != GameMode.CREATIVE) {
            event.isCancelled = true

            when (slot) {
                37 -> {

                }

                36 -> {
                    characterSelection.playerTeam.remove(player.uniqueId)
                    characterSelection.resetPlayer(player)
                }

                else -> {
                    characterSelection.playerTeam[player.uniqueId]?.let { safeTeamName ->
                        for (handler in handlers) {
                            if (handler.canHandle(safeTeamName)) {
                                /*
                                if (event.isRightClick) {
                                    CharacterSelection().resetPlayer(player)
                                }
                                */

                                if (handler.skillSelectionInventory[slot]?.skillCategory != "") {
                                    handler.switchSkill(
                                            player.uniqueId,
                                            handler.skillSelectionInventory[slot]?.skillCategory.toString(),
                                            handler.skillSelectionInventory[slot]?.skillName.toString()
                                    )
                                }
                                return
                            }
                        }
                        FighterlessInventoryHandler.getFighterSelectionInventory()[slot]?.skillName?.let { fighter ->
                            FighterlessInventoryHandler.switchClass(player.uniqueId, fighter, handlers, characterSelection)

                            for (handler in handlers) {
                                if (handler.canHandle(fighter)) {
                                    characterSelection.buildInventory(player, handler.skillSelectionInventory)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
