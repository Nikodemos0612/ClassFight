package me.nikodemos612.classfight.game

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import io.papermc.paper.event.player.PlayerPickItemEvent
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.block.BlockFace
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

private object SpawnLocation {
    const val X = 30.5
    const val Y = -42.9
    const val Z = 41.5
}
private const val INVULNERABILITY_DURATION = 150

/**
 * This class handles all the rules necessary to make the game run properly
 * @author Nikodemos0612 (Lucas Coimbra)
 */
class GameRulesHandler: Listener {
    @EventHandler
    fun onPlayerJoin(event : PlayerJoinEvent) {
        val player = event.player
        player.teleport(Location(player.world, SpawnLocation.X, SpawnLocation.Y, SpawnLocation.Z))
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.NIGHT_VISION,
                PotionEffect.INFINITE_DURATION,
                1,
                false,
                false
            )
        )
        player.maximumNoDamageTicks = 0
        player.noDamageTicks = 0
        player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = 1.0
        event.player.flySpeed = 0.1F
    }

    @EventHandler
    fun onPlayerInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked
        if (player is Player && event.clickedInventory != null && player.gameMode != GameMode.CREATIVE)
            event.isCancelled = true
    }

    @EventHandler
    fun onPlayerItemPickup(event: PlayerPickItemEvent) {
        if (event.player.gameMode != GameMode.CREATIVE)
            event.isCancelled = true
    }

    @EventHandler
    fun onPlayerItemDrop(event: PlayerDropItemEvent) {
        if (event.player.gameMode != GameMode.CREATIVE)
            event.isCancelled = true
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (event.player.gameMode != GameMode.CREATIVE)
            event.drops.removeAll{true}
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerPostRespawnEvent) {
        val player = event.player
        player.clearActivePotionEffects()
        player.addPotionEffect(PotionEffect(
            PotionEffectType.DAMAGE_RESISTANCE,
            INVULNERABILITY_DURATION,
            5
        ))
        player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 1))
        player.maximumNoDamageTicks = 0
        player.noDamageTicks = 0
        player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = 1.0
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        event.player.let { player ->

            if (player.gameMode != GameMode.CREATIVE) {
                if (player.location.block.getRelative(BlockFace.DOWN).type == Material.ORANGE_CONCRETE) {
                    player.damage(100000.0)
                }
            }
        }
    }

    @EventHandler
    fun onPlayerHungerLevelChange(event: FoodLevelChangeEvent) {
        if (event.entity is Player)
            event.isCancelled = true
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        (event.entity as? Player)?.let { safePlayer ->
            when (event.cause) {
                EntityDamageEvent.DamageCause.FALL -> {
                    if (event.damage <= 4 || safePlayer.hasPotionEffect(PotionEffectType.JUMP))
                        event.isCancelled = true
                }

                else -> {}
            }
        }
    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val entity = event.entity
        if (entity.type == EntityType.ARROW && entity.isOnGround)
            entity.remove()
    }
}