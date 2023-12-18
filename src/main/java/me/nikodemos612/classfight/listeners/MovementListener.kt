package me.nikodemos612.classfight.listeners

import me.nikodemos612.classfight.fighters.FighterHandlerManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class MovementListener : Listener{
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        FighterHandlerManager.runPlayerMovementHandler(event)
    }
}