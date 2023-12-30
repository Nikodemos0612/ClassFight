package me.nikodemos612.classfight.fighters

import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent

interface DefaultFighterHandler {
    fun canHandle(teamName: String) : Boolean
    fun onItemHeldChange(event: PlayerItemHeldEvent)
    fun onPlayerInteraction(event: PlayerInteractEvent)
    fun onProjectileHit(event: ProjectileHitEvent)
}
