package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.fighters.DefaultFighterHandler
import me.nikodemos612.classfight.utill.HealPlayerUseCase
import me.nikodemos612.classfight.utill.cooldown.Cooldown
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.Arrow
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import kotlin.collections.HashMap

private const val SHOTGUN_PROJECTILE_NAME = "shotgunShot"
private const val SHOTGUN_SHOT_COOLDOWN = 7500L
private const val SHOTGUN_PROJECTILE_DURATION = 3L
private const val SHOTGUN_PROJECTILE_SPEED = 10F
private const val SHOTGUN_PROJECTILE_DAMAGE = 1.0
private const val SHOTGUN_PROJECTILE_AMOUNT = 16
private const val SHOTGUN_PROJECTILE_SPREAD = 20F
private const val SHOTGUN_DASH_Y = 0.5
private const val SHOTGUN_DASH_STRENGTH = 1.0
private const val SHOTGUN_DASH_COOLDOWN = 7000L

private const val SHOTGUN_MINI_BASE_AMMO = 2
private const val SHOTGUN_MINI_ADD_AMMO = 2
private const val SHOTGUN_MINI_BASE_COOLDOWN = 1000L
private const val SHOTGUN_MINI_ADD_COOLDOWN_RATIO = 2.0F
private const val SHOTGUN_MINI_PROJECTILE_DURATION = 4L
private const val SHOTGUN_MINI_PROJECTILE_SPEED = 10F
private const val SHOTGUN_MINI_PROJECTILE_AMOUNT = 8
private const val SHOTGUN_MINI_PROJECTILE_SPREAD = 10F
private const val SHOTGUN_MINI_DASH_Y = 10.0
private const val SHOTGUN_MINI_DASH_STRENGTH  = 0.8
private const val SHOTGUN_MINI_DASH_COOLDOWN = 4500L

private const val PISTOL_PROJECTILE_NAME = "pistolShot"
private const val PISTOL_SHOT_COOLDOWN = 11000L
private const val PISTOL_PROJECTILE_SPEED = 2F
private const val PISTOL_PROJECTILE_DAMAGE = 8.0
private const val PISTOL_HEAL_EFFECT_STRENGTH = 12.0
private const val PISTOL_PULL_STRENGTH = 0.4


/**
 * This class handles the ShotgunnerFighter and all it's events.
 * @author Gumend3s (Gustavo Mendes)
 * @see Cooldown
 * @see DefaultFighterHandler
 */
class ShotgunnerFighterHandler(private val plugin: Plugin) : DefaultFighterHandler() {

    private val shotgunCooldown = Cooldown()
    private val pistolCooldown = Cooldown()
    private val dashCooldown = Cooldown()

    override val fighterTeamName = "shotgunner"

    override fun resetInventory(player: Player) {
        player.inventory.clear()
        player.inventory.setItem(0, ItemStack(Material.STICK))
        player.inventory.setItem(1, ItemStack(Material.TRIPWIRE_HOOK))
        player.inventory.setItem(2, ItemStack(Material.ENDER_PEARL))
        player.inventory.heldItemSlot = 0

        player.allowFlight = true
        player.flySpeed = 0F

        if (player.gameMode == GameMode.CREATIVE) {
            player.flySpeed = 1F
        }
    }

    override fun resetCooldowns(player: Player) {
        val playerUUID = player.uniqueId

        shotgunCooldown.resetCooldown(playerUUID)
        pistolCooldown.resetCooldown(playerUUID)
        dashCooldown.resetCooldown(playerUUID)
        player.resetCooldown()
    }

    override fun onItemHeldChange(event: PlayerItemHeldEvent) {
        val player = event.player

        when (event.newSlot) {
            0 -> {

            }

            else -> {
                player.inventory.heldItemSlot = 0
            }
        }
    }

    override fun onPlayerInteraction(event: PlayerInteractEvent) {
        val player = event.player

        if (event.action.isLeftClick && !shotgunCooldown.hasCooldown(player.uniqueId)) {
            when (player.inventory.getItem(0)?.type) {
                Material.STICK -> {
                    shootShotgun(player)
                    Bukkit.getServer().scheduler.runTaskLater(plugin, endShotgunShot(player), SHOTGUN_PROJECTILE_DURATION)
                }

                Material.BLAZE_ROD -> {
                    shootShotgun(player)
                    Bukkit.getServer().scheduler.runTaskLater(plugin, endShotgunShot(player), SHOTGUN_MINI_PROJECTILE_DURATION)
                }

                else -> { }
            }
        }

        if (event.action.isRightClick && !pistolCooldown.hasCooldown(player.uniqueId)) {
            shootPistol(player)
        }

    }

    override fun onPlayerHitByEntityFromThisTeam(event: EntityDamageByEntityEvent) {
        (event.damager as? Projectile)?.let { projectile ->
            when (projectile.customName()) {
                Component.text(SHOTGUN_PROJECTILE_NAME) -> {
                    event.damage = SHOTGUN_PROJECTILE_DAMAGE
                }

                Component.text(PISTOL_PROJECTILE_NAME) -> {
                    event.damage = PISTOL_PROJECTILE_DAMAGE
                    (projectile.shooter as? Player)?.let {  shooter ->
                        HealPlayerUseCase(shooter, PISTOL_HEAL_EFFECT_STRENGTH)

                        val velocity = shooter.location.toVector().subtract(event.entity.location.toVector())
                        event.entity.velocity = velocity.setY(velocity.y.coerceAtLeast(0.25))
                            .multiply(PISTOL_PULL_STRENGTH)
                        addMiniShotgun(shooter)
                    }
                }

                else -> {}
            }
        }
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player

        if (event.player.isOnGround) {
            player.removePotionEffect(PotionEffectType.JUMP)
        }

        if (event.player.isFlying) {
            if (!dashCooldown.hasCooldown(player.uniqueId)) {
                when (player.inventory.getItem(0)?.type) {
                    Material.STICK -> {
                        horizontalDash(player)
                    }

                    Material.BLAZE_ROD , Material.BRUSH -> {
                        verticalDash(player)
                    }

                    else -> { }
                }
            }
            event.player.isFlying = false
        }
    }

    override fun onPlayerDamage(event: EntityDamageEvent) {
        (event.entity as? Player)?.removePotionEffect(PotionEffectType.JUMP)
    }

    /**
     * Responsible for shooting either the shotgun or the mini, based on which item the player has on it's shotgun slot
     *
     * @param Player The player that is shooting the shotgun
     */
    private fun shootShotgun(player: Player) {
        when (player.inventory.getItem(0)?.type) {
            Material.STICK -> {
                for(i in 1..SHOTGUN_PROJECTILE_AMOUNT) {
                    player.world.spawnArrow(
                            player.eyeLocation,
                            player.eyeLocation.direction,
                            SHOTGUN_PROJECTILE_SPEED,
                            SHOTGUN_PROJECTILE_SPREAD
                    ).let{
                        it.shooter = player
                        it.customName(Component.text(SHOTGUN_PROJECTILE_NAME))
                        it.setGravity(false)
                    }
                }

                shotgunCooldown.addCooldownToPlayer(player.uniqueId, SHOTGUN_SHOT_COOLDOWN)
                player.setCooldown(player.inventory.getItem(0)?.type ?: Material.BEDROCK, (SHOTGUN_SHOT_COOLDOWN/50).toInt())
            }

            Material.BLAZE_ROD -> {
                for(i in 1..SHOTGUN_MINI_PROJECTILE_AMOUNT) {
                    player.world.spawnArrow(
                            player.eyeLocation,
                            player.eyeLocation.direction,
                            SHOTGUN_MINI_PROJECTILE_SPEED,
                            SHOTGUN_MINI_PROJECTILE_SPREAD
                    ).let{
                        it.shooter = player
                        it.customName(Component.text(SHOTGUN_PROJECTILE_NAME))
                        it.setGravity(false)
                    }
                }

                player.inventory.removeItem(ItemStack(Material.BLAZE_ROD, 1))

                when (player.inventory.getItem(0)?.type) {
                    Material.BLAZE_ROD -> {
                        shotgunCooldown.addCooldownToPlayer(player.uniqueId, SHOTGUN_MINI_BASE_COOLDOWN)
                        player.setCooldown(player.inventory.getItem(0)?.type ?: Material.BEDROCK, (SHOTGUN_MINI_BASE_COOLDOWN/50).toInt())
                    }

                    else -> {
                        player.inventory.setItem(2, ItemStack(Material.ENDER_PEARL))
                        player.setCooldown(Material.ENDER_PEARL, 0)
                        dashCooldown.resetCooldown(player.uniqueId)

                        player.inventory.setItem(0, ItemStack(Material.STICK))
                        shotgunCooldown.addCooldownToPlayer(player.uniqueId, SHOTGUN_SHOT_COOLDOWN)
                        player.setCooldown(player.inventory.getItem(0)?.type ?: Material.BEDROCK, (SHOTGUN_SHOT_COOLDOWN/50).toInt())
                    }
                }
            }
            
            else -> { }
        }
    }

    /**
     * Is always called after the shootShotgun function and is responsible for deleting the projectiles from the shotgun
     *
     * @see shootShotgun
     *
     * @param Player The player that is shooting the shotgun
     */
    private fun endShotgunShot(player: Player) = Runnable {
        for (entity in player.world.entities) {
            if (entity.customName() == Component.text(SHOTGUN_PROJECTILE_NAME) && entity is Arrow) {
                val shooter = entity.shooter

                if (shooter is Player && shooter.uniqueId == player.uniqueId) {
                    entity.remove()
                }
            }
        }
    }

    /**
     * Responsible for adding stacks of the mini shotgun to the player, and changing its dash for the vertical one
     *
     * @param Player The player that is shooting the shotgun
     */
    private fun addMiniShotgun(player: Player) {
        if (player.inventory.getItem(0)?.type != Material.BLAZE_ROD ) {
            player.inventory.setItem(0, ItemStack(Material.BLAZE_ROD, SHOTGUN_MINI_BASE_AMMO))
        } else {
            player.inventory.addItem(ItemStack(Material.BLAZE_ROD, SHOTGUN_MINI_ADD_AMMO))
        }

        player.setCooldown(Material.BLAZE_ROD, ((SHOTGUN_MINI_BASE_COOLDOWN + (
                (shotgunCooldown.returnCooldown(player.uniqueId)) / SHOTGUN_MINI_ADD_COOLDOWN_RATIO).toLong()
                )/50).toInt())
        shotgunCooldown.addCooldownToPlayer(player.uniqueId, SHOTGUN_MINI_BASE_COOLDOWN +  (
                (shotgunCooldown.returnCooldown(player.uniqueId)) / SHOTGUN_MINI_ADD_COOLDOWN_RATIO).toLong()
        )

        player.inventory.setItem(2, ItemStack(Material.ENDER_EYE))
        player.setCooldown(Material.ENDER_EYE, 0)
        dashCooldown.resetCooldown(player.uniqueId)
    }

    /**
     * Responsible for shooting the pistol
     *
     * @param Player The player that is shooting the shotgun
     */
    private fun shootPistol(player: Player) {
        player.launchProjectile(Arrow::class.java, player.location.direction.multiply(PISTOL_PROJECTILE_SPEED)).let{
            it.shooter = player
            it.customName(Component.text(PISTOL_PROJECTILE_NAME))
            it.setGravity(false)
        }

        pistolCooldown.addCooldownToPlayer(player.uniqueId, PISTOL_SHOT_COOLDOWN)
        player.setCooldown(Material.TRIPWIRE_HOOK, (PISTOL_SHOT_COOLDOWN/50).toInt())
    }

    /**
     * Responsible for doing the horizontal dash
     *
     * @param Player The player that is shooting the shotgun
     */
    private fun horizontalDash(player: Player) {
        player.velocity = player.eyeLocation.direction.setY(0).normalize().setY(SHOTGUN_DASH_Y).normalize().multiply(SHOTGUN_DASH_STRENGTH)
        dashCooldown.addCooldownToPlayer(player.uniqueId, SHOTGUN_DASH_COOLDOWN)
        player.setCooldown(player.inventory.getItem(2)?.type ?: Material.BEDROCK, (SHOTGUN_DASH_COOLDOWN/50).toInt())
        player.addPotionEffect(
                PotionEffect(
                        PotionEffectType.JUMP,
                        PotionEffect.INFINITE_DURATION,
                        1,
                        false,
                        false,
                        true,
                ),
        )
    }

    /**
     * Responsible for doing the vertical dash
     *
     * @param Player The player that is shooting the shotgun
     */
    private fun verticalDash(player: Player) {
        player.velocity = player.eyeLocation.direction.setY(0).normalize().setY(SHOTGUN_MINI_DASH_Y).normalize().multiply(SHOTGUN_MINI_DASH_STRENGTH)
        dashCooldown.addCooldownToPlayer(player.uniqueId, SHOTGUN_MINI_DASH_COOLDOWN)
        player.setCooldown(player.inventory.getItem(2)?.type ?: Material.BEDROCK, (SHOTGUN_MINI_DASH_COOLDOWN/50).toInt())
        player.addPotionEffect(
                PotionEffect(
                        PotionEffectType.JUMP,
                        PotionEffect.INFINITE_DURATION,
                        1,
                        false,
                        false,
                        true,
                ),
        )
    }

}