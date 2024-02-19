package me.nikodemos612.classfight.ui

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.plugin.Plugin
import java.util.*
import kotlin.collections.HashMap

/*
class CharacterSelection: Listener {

    @EventHandler
    fun onPlayerInventoryClick(event: InventoryClickEvent): Int {
        val player = event.whoClicked
        if (player is Player && event.clickedInventory != null && player.gameMode != GameMode.CREATIVE) {
            event.isCancelled = true

            return event.slot
        }
        return -1
    }
}*/