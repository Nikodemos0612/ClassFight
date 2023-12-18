package me.nikodemos612.classfight.fighters

import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent

interface DefaultFighterHandler {
    fun canHandle(teamName: String) : Boolean
    fun onItemHeldChange(event: PlayerItemHeldEvent)
    fun onPlayerMove(event: PlayerMoveEvent)
    fun onInventoryClick(event: InventoryClickEvent)
    fun onPlayerInteraction(event: PlayerInteractEvent)
}
