package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.utill.RunInLineBetweenTwoLocationsUseCase
import me.nikodemos612.classfight.utill.cooldown.Cooldown
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import org.bukkit.util.Vector

private const val NORMAL_ARROW_MULTIPLIER = 2
private const val ZOOM_ARROW_MULTIPLIER = 5
private const val ARROW_KNOCKBACK_MULTIPLIER = -0.7
private const val ZOOM_KNOCKBACK_MULTIPLIER = -0.2
private const val NORMAL_SHOT_COOLDOWN = 2500L
private const val NORMAL_SHOOT_DAMAGE = 4.0
private const val NORMAL_SHOOT_NAME = "normalShot"
private const val ZOOM_SHOOT_DAMAGE = 10.0
private const val ZOOM_SHOOT_NAME = "zoomShot"
private const val ZOOM_SHOT_COOLDOWN = 8000L
private const val ZOOM_COOLDOWN = 10L
private const val ZOOM_SHOT_COOLDOWN_REMOVAL_ON_HIT = 4000L
private const val HEAL_COOLDOWN = 30000L
private const val HEAL_COOLDOWN_REMOVAL_ON_HIT = 10000L

/**
 * This class handles the SniperFighter an all it's events.
 * @author Nikodemos0612 (Lucas Coimbra)
 * @see Cooldown
 * @see DefaultFighterHandler
 */
class SniperFighterHandler(private val plugin: Plugin): DefaultFighterHandler() {

    private val shotCooldown = Cooldown()
    private val zoomCooldown = Cooldown()
    private val playerHealCooldown = Cooldown()
    private val playersOnZoom = HashMap<UUID, Int>()

    override val fighterTeamName = "sniper"

    override fun resetInventory(player: Player) {
        player.inventory.clear()
        player.inventory.setItem(0, ItemStack(Material.STICK))
        player.inventory.setItem(1, ItemStack(Material.BRUSH))

        player.flySpeed = 0.1F
    }

    override fun resetCooldowns(player: Player) {
        val playerUUID = player.uniqueId

        shotCooldown.resetCooldown(playerUUID)
        zoomCooldown.resetCooldown(playerUUID)
        playerHealCooldown.resetCooldown(playerUUID)
        playersOnZoom[playerUUID]?.let {
            Bukkit.getScheduler().cancelTask(it)
            playersOnZoom.remove(playerUUID)
        }
        player.resetCooldown()
    }

    override fun onItemHeldChange(event: PlayerItemHeldEvent) {
        val player = event.player

        when (event.newSlot) {
            1 -> {
                if (!playerHealCooldown.hasCooldown(player.uniqueId)) {
                    useHealAbility(player)
                }
                player.inventory.heldItemSlot = 0
            }
        }
    }

    override fun onPlayerInteraction(event: PlayerInteractEvent) {
        val player = event.player

        when {
            event.action.isLeftClick -> {
                if (!shotCooldown.hasCooldown(player.uniqueId)) {
                    if (hasZoom(player.uniqueId)) {
                        shootWithZoom(player)
                        zoomOut(player)
                    } else shootWithoutZoom(player)
                }
            }

            event.action.isRightClick -> {
                if (!zoomCooldown.hasCooldown(player.uniqueId)) {
                    if (hasZoom(player.uniqueId))
                        zoomOut(player)
                    else zoomIn(player)
                }
            }
        }
    }

    override fun onPlayerHitByEntityFromThisTeam(event: EntityDamageByEntityEvent) {
        (event.damager as? Projectile)?.let { projectile ->
            when (projectile.customName()) {
                Component.text(NORMAL_SHOOT_NAME) -> {
                    event.damage = NORMAL_SHOOT_DAMAGE
                }

                Component.text(ZOOM_SHOOT_NAME) -> {
                    event.damage = ZOOM_SHOOT_DAMAGE

                    (projectile.shooter as? Player)?.let { player ->
                        val cooldownOnShot = ((shotCooldown.returnCooldown(player.uniqueId)) -
                                ZOOM_SHOT_COOLDOWN_REMOVAL_ON_HIT).coerceAtLeast(1)

                        val cooldownOnHeal = ((playerHealCooldown.returnCooldown(player.uniqueId)) -
                                HEAL_COOLDOWN_REMOVAL_ON_HIT).coerceAtLeast(1)

                        shotCooldown.addCooldownToPlayer(
                            player.uniqueId,
                            cooldownOnShot
                        )
                        playerHealCooldown.addCooldownToPlayer(
                            player.uniqueId,
                            cooldownOnHeal
                        )

                        player.resetCooldown()
                        player.setCooldown(Material.STICK, (cooldownOnShot / 50).toInt())
                        player.setCooldown(Material.BRUSH, (cooldownOnHeal / 50).toInt())

                        RunInLineBetweenTwoLocationsUseCase(
                            location1 = player.location,
                            location2 = event.entity.location,
                            stepFun = { location : Vector ->
                                player.world.spawnParticle(
                                    Particle.REDSTONE,
                                    location.x,
                                    location.y,
                                    location.z,
                                    1,
                                    Particle.DustOptions(Color.AQUA, 2f)
                                )
                            },
                            stepSize = 0.5,
                        )

                        player.playSound(player, Sound.BLOCK_CONDUIT_ACTIVATE, 10f, 1f)
                        (event.entity as? Player)?.let { damaged ->
                            damaged.playSound(damaged, Sound.BLOCK_CONDUIT_DEACTIVATE, 10f, 1f)
                        }
                    }
                }

                else -> {}
            }
        }
    }

    /**
     * This function is responsible to make the given player shoot an arrow with the expected values when it's not on
     * zoom.
     *
     * @param player the player that will shoot the arrow.
     */
    private fun shootWithoutZoom(player: Player) {
        player.launchProjectile(Arrow::class.java, player.location.direction.multiply(NORMAL_ARROW_MULTIPLIER)).let{
            it.shooter = player
            it.customName(Component.text(NORMAL_SHOOT_NAME))
        }
        player.velocity = player.location.direction.multiply(ARROW_KNOCKBACK_MULTIPLIER)

        shotCooldown.addCooldownToPlayer(player.uniqueId, NORMAL_SHOT_COOLDOWN)
        player.setCooldown(Material.STICK, (NORMAL_SHOT_COOLDOWN / 50).toInt())
    }

    /**
     * This function is responsible to make the given player shoot an arrow with the expected values when it's on zoom.
     *
     * @param player the player that will shoot the arrow.
     */
    private fun shootWithZoom(player: Player) {
        player.launchProjectile(Arrow::class.java, player.location.direction.multiply(ZOOM_ARROW_MULTIPLIER)).let {
            it.shooter = player
            it.customName(Component.text(ZOOM_SHOOT_NAME))
            it.velocity.multiply(ZOOM_ARROW_MULTIPLIER)
            it.setGravity(false)
            it.pierceLevel = 5
        }
        player.velocity = player.location.direction.multiply(ZOOM_KNOCKBACK_MULTIPLIER)

        shotCooldown.addCooldownToPlayer(player.uniqueId, ZOOM_SHOT_COOLDOWN)
        player.setCooldown(Material.STICK, (ZOOM_SHOT_COOLDOWN / 50).toInt())
        player.playSound(player, Sound.ITEM_TRIDENT_THUNDER, 10f, 1f)
    }

    /**
     * Responsible to give the zoom effect to the player.
     *
     * @param player the player that will receive the zoom effect.
     */
    private fun zoomIn(player: Player) {
        val zoomTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(
            plugin,
            zoomInEffectTask(player),
            0L,
            2L
        )
        playersOnZoom[player.uniqueId] = zoomTask

        zoomCooldown.addCooldownToPlayer(player.uniqueId, ZOOM_COOLDOWN)
        player.addPotionEffect(
                PotionEffect(
                        PotionEffectType.SLOW,
                        PotionEffect.INFINITE_DURATION,
                        49,
                        true,
                        false
                )
        )
        player.playSound(player, Sound.ITEM_SPYGLASS_USE, 10f, 1f)
    }

    /**
     * Responsible to remove the zoom effect from the player.
     *
     * @param player the player that will have its zoom effect removed
     */
    private fun zoomOut(player: Player) {
        playersOnZoom[player.uniqueId]?.let{ Bukkit.getScheduler().cancelTask(it) }
        playersOnZoom.remove(player.uniqueId)
        zoomCooldown.addCooldownToPlayer(player.uniqueId, ZOOM_COOLDOWN)
        player.removePotionEffect(PotionEffectType.SLOW)
        player.playSound(player, Sound.ITEM_SPYGLASS_STOP_USING, 10f, 1f)
    }

    /**
     * Verifies if the given player has the zoom effect
     * @param player the player that will be verified
     * @return a boolean that represents if the player has the zoom effect.
     */
    private fun hasZoom(player: UUID): Boolean = playersOnZoom.contains(player)

    /**
     * Gives the heal effect to the player.
     * @param player the player that will receive the heal effect.
     */
    private fun useHealAbility(player: Player) {
        player.health = 20.0
        player.setCooldown(Material.BRUSH, (HEAL_COOLDOWN/ 50).toInt())
        playerHealCooldown.addCooldownToPlayer(player.uniqueId, HEAL_COOLDOWN)
        player.playSound(player, Sound.ENTITY_MOOSHROOM_CONVERT, 10f, 1f)
    }

    private fun zoomInEffectTask(player: Player) = Runnable {
        val direction = player.eyeLocation.direction
        val lookingLocationDistance = player.getTargetBlockExact(100)?.location?.distance(player.eyeLocation)

        RunInLineBetweenTwoLocationsUseCase(
            location1 = player.eyeLocation.add(direction.multiply(2)),
            location2 = lookingLocationDistance?.let {
                player.eyeLocation.add(player.eyeLocation.direction.multiply(it))
            } ?: player.eyeLocation.add(player.eyeLocation.direction.multiply(100)),
            stepSize = 2.0,
            stepFun = { location: Vector ->
                player.world.spawnParticle(
                    Particle.DUST_COLOR_TRANSITION,
                    location.x,
                    location.y,
                    location.z,
                    1,
                    Particle.DustTransition(Color.RED, Color.AQUA, 1f)
                )
            },
        )
    }
}