package me.nikodemos612.classfight.fighters.handlers

import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent

/**
 * This interface has all the events and functions that should be implemented by every Fighter class.
 * In case that you need a new event, just add it here and into the FighterHandlerListeners class.
 *
 * Obligatory to implement in your class:
 * * canHandle
 * * resetInventory
 * * resetCooldowns (if there is any)
 *
 * The rest are the event that you can use to build your own fighter.
 *
 * @author Nikodemos0612 (Lucas Coimbra)
 */
open class DefaultFighterHandler {
    open fun canHandle(teamName: String) : Boolean = false
    open fun resetInventory(player: Player) {}
    open fun resetCooldowns(player: Player) {}
    open fun onItemHeldChange(event: PlayerItemHeldEvent) {}
    open fun onPlayerInteraction(event: PlayerInteractEvent) {}
    open fun onProjectileHit(event: ProjectileHitEvent) {}
    open fun onPlayerHitByEntityFromThisTeam(event: EntityDamageByEntityEvent) {}
    open fun onPlayerMove(event: PlayerMoveEvent) {}
    open fun onPlayerDamage(event: EntityDamageEvent) {}
}
