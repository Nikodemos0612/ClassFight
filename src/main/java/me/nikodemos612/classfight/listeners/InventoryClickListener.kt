package me.nikodemos612.classfight.listeners

import me.nikodemos612.classfight.fighters.FighterHandlerManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class InventoryClickListener: Listener {
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        FighterHandlerManager.runInventoryCLickHandler(event)
    }
}