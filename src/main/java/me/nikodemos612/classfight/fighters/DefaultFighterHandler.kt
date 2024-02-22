package me.nikodemos612.classfight.fighters

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.meta.ItemMeta
import java.util.UUID
import javax.annotation.Nullable

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
abstract class DefaultFighterHandler {

    protected abstract val fighterTeamName: String
    open val walkSpeed = 0.2F
    fun canHandle(teamName: String) : Boolean = teamName == fighterTeamName
    /* abstract */open fun setSkills(player: Player, skills: HashMap<String, String>) {}
    abstract fun resetInventory(player: Player)
    abstract fun resetCooldowns(player: Player)

    open fun onItemHeldChange(event: PlayerItemHeldEvent) {}
    open fun onPlayerInteraction(event: PlayerInteractEvent) {}
    open fun onProjectileHit(event: ProjectileHitEvent) {}
    open fun onPlayerHitByEntityFromThisTeam(event: EntityDamageByEntityEvent) {}
    open fun onPlayerMove(event: PlayerMoveEvent) {}
    open fun onPlayerDamage(event: EntityDamageEvent) {}
}
