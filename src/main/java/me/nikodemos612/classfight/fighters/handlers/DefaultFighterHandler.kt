package me.nikodemos612.classfight.fighters.handlers

import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent

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
interface DefaultFighterHandler {
    fun canHandle(teamName: String) : Boolean
    fun resetInventory(player: Player)
    fun resetCooldowns(player: Player)
    fun onItemHeldChange(event: PlayerItemHeldEvent)
    fun onPlayerInteraction(event: PlayerInteractEvent)
    fun onProjectileHit(event: ProjectileHitEvent)
    fun onPlayerHitByEntityFromThisTeam(event: EntityDamageByEntityEvent)
}
