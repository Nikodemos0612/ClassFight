package me.nikodemos612.classfight.game

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import io.papermc.paper.event.player.PlayerPickItemEvent
import me.nikodemos612.classfight.fighters.handlers.FangsPublicArgs
import me.nikodemos612.classfight.utill.RunInLineBetweenTwoLocationsUseCase
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.block.BlockFace
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.EntityType
import org.bukkit.entity.EvokerFangs
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
import org.bukkit.util.Vector

private object SpawnLocation {
    const val X = 30.5
    const val Y = -42.9
    const val Z = 41.5
}
private const val INVULNERABILITY_DURATION = 100

/**
 * This class handles all the rules necessary to make the game run properly
 * @author Nikodemos0612 (Lucas Coimbra)
 */
class GameRulesHandler: Listener {
    @EventHandler
    fun onPlayerJoin(event : PlayerJoinEvent) {
        val player = event.player
        player.teleport(Location(player.world, SpawnLocation.X, SpawnLocation.Y, SpawnLocation.Z))
        player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 1, false,false))
        player.maximumNoDamageTicks = 0
        player.noDamageTicks = 0
        player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = 1.0
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

                player.getPotionEffect(FangsPublicArgs.JAIL_EFFECT)?.let { playerPotionEffect ->
                    handleJailEffect(playerPotionEffect, player)
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
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if(event.entity is Player && event.damager is Player) {
            when (event.cause) {
                EntityDamageEvent.DamageCause.ENTITY_ATTACK-> {
                    event.damage = 0.0
                }

                else -> {}
            }
        }

        when (event.damager) {
            is EvokerFangs -> {
                event.damage = FangsPublicArgs.FANG_DAMAGE
            }
        }

    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val entity = event.entity
        if (entity.type == EntityType.ARROW && entity.isOnGround)
            entity.remove()
    }

    private fun handleJailEffect(playerPotionEffect: PotionEffect, player: Player) {
        val closeEntities = FangsPublicArgs.JAILED_AREA.let { player.getNearbyEntities(it, it, it) }

        for (entity in closeEntities) {
            if (
                entity is AreaEffectCloud &&
                entity.customName() == Component.text(FangsPublicArgs.JAIL_NAME) &&
                entity.duration - entity.ticksLived >= playerPotionEffect.duration - FangsPublicArgs.JAIL_EFFECT_DURATION
                    - 10 &&
                entity.duration - entity.ticksLived <= playerPotionEffect.duration + FangsPublicArgs.JAIL_EFFECT_DURATION
                    + 10
                    ) {
                RunInLineBetweenTwoLocationsUseCase(
                    location1 = player.location,
                    location2 = entity.location,
                    stepSize = 0.5,
                    stepFun =  { location : Vector ->
                        player.world.spawnParticle(
                            Particle.REDSTONE,
                            location.x,
                            location.y,
                            location.z,
                            1,
                            Particle.DustOptions(Color.BLACK, 1F)
                        )
                    },
                )

                val jailRadius = entity.radius
                val jailLocation = entity.location

                val distanceDifference = player.location.subtract(jailLocation)
                if (
                    distanceDifference.y > jailRadius || distanceDifference.y < -jailRadius ||
                    distanceDifference.x > jailRadius || distanceDifference.x < -jailRadius ||
                    distanceDifference.z > jailRadius || distanceDifference.z < -jailRadius
                ) {

                    val playerVelocity = player.velocity
                    player.velocity = playerVelocity
                        .setX(playerVelocity.x.coerceAtLeast(-0.2).coerceAtMost(0.2))
                        .setY(playerVelocity.y.coerceAtLeast(-0.2).coerceAtMost(0.2))
                        .setZ(playerVelocity.z.coerceAtLeast(-0.2).coerceAtMost(0.2))
                        .add(distanceDifference.toVector().multiply(-0.05))

                   RunInLineBetweenTwoLocationsUseCase(
                    location1 = player.location,
                    location2 = entity.location,
                    stepSize = 0.5,
                    stepFun =  { location : Vector ->
                        player.world.spawnParticle(
                            Particle.DUST_COLOR_TRANSITION,
                            location.x,
                            location.y,
                            location.z,
                            1,
                            Particle.DustTransition(Color.RED, Color.BLACK, 2f)
                        )
                    },
                )
                }
            }
        }
    }

}