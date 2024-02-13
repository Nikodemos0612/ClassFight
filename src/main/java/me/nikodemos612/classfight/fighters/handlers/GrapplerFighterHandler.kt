package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.fighters.DefaultFighterHandler
import me.nikodemos612.classfight.utill.HealPlayerUseCase
import me.nikodemos612.classfight.utill.cooldown.Cooldown
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.*
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

//base = 0.2
private const val PLAYER_WALKSPEED = 0.18F

private const val GRAPPLE_PROJECTILE_NAME = "grappleShot"
private const val GRAPPLE_PROJECTILE_SPEED = 3F
private const val GRAPPLE_PROJECTILE_DAMAGE = 4.0
private const val GRAPPLE_SHOT_COOLDOWN = 4500L
private const val GRAPPLE_PULL_STRENGTH = 1.5
private const val GRAPPLE_PULL_MAX_Y = 2.7
private const val GRAPPLE_PULL_MIN_Y = 0.25

private const val DOUBLE_JUMP_COOLDOWN = 5500L
private const val DOUBLE_JUMP_STRENGTH = 1.0
private const val DOUBLE_JUMP_Y = 1.1

private const val SLASH_ATTACK_COOLDOWN = 4500L
private const val SLASH_ATTACK_RADIUS = 3.0
private const val SLASH_ATTACK_HEIGHT = 1.25
private const val SLASH_PARTICLE_AMOUNT = 1000
private const val SLASH_HEAL_AMOUNT = 3.0
private const val SLASH_BASE_DAMAGE_AMOUNT = 3.0
private const val SLASH_ADD_DAMAGE_AMOUNT = 3.0
private const val SLASH_KNOCKBACK_STRENGTH = 1.5F
private const val SLASH_KNOCKBACK_MAX_Y = 3.0
private const val SLASH_KNOCKBACK_MIN_Y = 0.35
private const val SLASH_JUMP_STRENGTH = 0.8
private const val SLASH_JUMP_Y = 0.9


/**
 * This class handles the GrapplerFighter and all it's events.
 * @author Gumend3s (Gustavo Mendes)
 * @see Cooldown
 * @see DefaultFighterHandler
 */
class GrapplerFighterHandler(private val plugin: Plugin) : DefaultFighterHandler() {

    private val grappleCooldown = Cooldown()
    private val slashCooldown = Cooldown()

    override val fighterTeamName = "grappler"
    override val walkSpeed = PLAYER_WALKSPEED

    override fun resetInventory(player: Player) {
        player.inventory.clear()
        player.inventory.setItem(0, ItemStack(Material.STICK))
        player.inventory.setItem(1, ItemStack(Material.NETHER_STAR))
        player.inventory.setItem(2, ItemStack(Material.RABBIT_FOOT))
        player.inventory.heldItemSlot = 0

        player.allowFlight = true
        player.flySpeed = 0F

        if (player.gameMode == GameMode.CREATIVE) {
            player.flySpeed = 1F
        }

        player.addPotionEffect(PotionEffect(PotionEffectType.JUMP, PotionEffect.INFINITE_DURATION, 1, false,false))
    }

    override fun resetCooldowns(player: Player) {
        val playerUUID = player.uniqueId

        grappleCooldown.resetCooldown(playerUUID)
        slashCooldown.resetCooldown(playerUUID)
        
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

        when {
            event.action.isLeftClick -> handleLeftClick(player)

            event.action.isRightClick -> handleRightClick(player)
        }
    }

    override fun onProjectileHit(event: ProjectileHitEvent) {

        val projectile = event.entity as? Projectile
        when (projectile?.type) {
            EntityType.ARROW -> {
                when (projectile.customName()) {
                    Component.text(GRAPPLE_PROJECTILE_NAME) -> {
                        pullPlayer(projectile)
                        (projectile.shooter as? Player)?.let {  shooter ->
                            shooter.inventory.setItem(0, ItemStack(Material.STICK, 1))
                            grappleCooldown.addCooldownToPlayer(shooter.uniqueId, GRAPPLE_SHOT_COOLDOWN)
                            shooter.setCooldown(Material.STICK, (GRAPPLE_SHOT_COOLDOWN/50).toInt())
                        }
                    }
                    else -> {}
                }
            }
            else -> {}
        }
        event.entity.remove()
    }

    override fun onPlayerHitByEntityFromThisTeam(event: EntityDamageByEntityEvent) {
        when (event.cause) {
            EntityDamageEvent.DamageCause.ENTITY_ATTACK -> (event.damager as? Player)?.let {
                handleLeftClick(it)
                event.damage = 0.0
            }

            else -> {
                when (val damager = event.damager) {
                    is Projectile -> {
                        when (damager.customName()) {
                            Component.text(GRAPPLE_PROJECTILE_NAME) -> {
                                event.damage = GRAPPLE_PROJECTILE_DAMAGE
                                (damager.shooter as? Player)?.let {shooter ->
                                    shooter.inventory.addItem(ItemStack(Material.NETHER_STAR, 1))
                                }
                            }

                            else -> {}
                        }
                    }

                    else -> {

                    }
                }
            }
        }
    }

    private fun handleLeftClick(player: Player) {
        if (!grappleCooldown.hasCooldown(player.uniqueId)) {
            when (player.inventory.getItem(0)?.type) {
                Material.STICK -> {
                    shootGrapple(player)
                }
                else -> {}
            }
        }
    }

    private fun handleRightClick(player: Player) {
        if (!slashCooldown.hasCooldown(player.uniqueId)) {
            attackSlash(player)
        }
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player

        if (player.isFlying) {
            player.playSound(player, Sound.BLOCK_PISTON_EXTEND, 10F, 1F)

            player.allowFlight = false
            player.flySpeed = 0.5F

            player.velocity = player.eyeLocation.direction.setY(0).normalize().setY(DOUBLE_JUMP_Y).normalize().multiply(DOUBLE_JUMP_STRENGTH)
            player.setCooldown(Material.RABBIT_FOOT, (DOUBLE_JUMP_COOLDOWN/50).toInt())
            Bukkit.getServer().scheduler.runTaskLater(plugin, resetDoubleJump(player), DOUBLE_JUMP_COOLDOWN/50)
        }
    }

    override fun onPlayerDamage(event: EntityDamageEvent) {
        val player = event.entity
    }

    /**
     * Called after the cooldown of the double jump has ended and is responsible for making the player able to use it
     * again
     *
     * @param Player The player that is shooting the shotgun
     */
    private fun resetDoubleJump(player: Player) = Runnable {
        player.allowFlight = true
        player.flySpeed = 0F
    }

    /**
     * Responsible for shooting the grapple
     *
     * @param Player The player that is shooting the shotgun
     */
    private fun shootGrapple(player: Player) {
        player.playSound(player, Sound.ENTITY_FISHING_BOBBER_THROW, 10F, 1F)

        player.launchProjectile(Arrow::class.java, player.location.direction.multiply(GRAPPLE_PROJECTILE_SPEED)).let{
            it.shooter = player
            it.customName(Component.text(GRAPPLE_PROJECTILE_NAME))
            it.setGravity(false)
        }

        player.inventory.setItem(0, ItemStack(Material.BARRIER, 1))
    }

    /**
     * Responsible for pulling the player towards the grapple impact point
     *
     * @param Projectile The grapple projectile
     */
    private fun pullPlayer(projectile: Projectile) {
        (projectile.shooter as? Player)?.let {  shooter ->
            shooter.playSound(shooter, Sound.ENTITY_CHICKEN_EGG, 10F, 1F)

            val velocity = projectile.location.toVector().subtract(shooter.location.toVector())
            shooter.velocity = velocity.setY(velocity.y.coerceAtMost(GRAPPLE_PULL_MAX_Y).coerceAtLeast(GRAPPLE_PULL_MIN_Y))
                    .normalize().multiply(GRAPPLE_PULL_STRENGTH)
        }
    }

    /**
     * Responsible for doing the slash attack
     *
     * @param Player The player that is shooting the shotgun
     */
    private fun attackSlash(player: Player) {
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 10F, 1F)

        val playerCenter = Location(player.world, player.location.x, player.location.y + 1, player.location.z)

        val slashParticle = Runnable {
            player.world.spawnParticle(
                    Particle.ELECTRIC_SPARK,
                    playerCenter,
                    SLASH_PARTICLE_AMOUNT,
                    SLASH_ATTACK_RADIUS / 2,
                    SLASH_ATTACK_HEIGHT / 2,
                    SLASH_ATTACK_RADIUS / 2,
                    0.0,
            )
        }

        for (i in 0..6) {
            Bukkit.getServer().scheduler.runTaskLater(plugin, slashParticle, i.toLong())
        }

        for (damageEntity in player.world.getNearbyPlayers(
                playerCenter,
                SLASH_ATTACK_RADIUS * 2,
                SLASH_ATTACK_HEIGHT * 2,
                SLASH_ATTACK_RADIUS * 2))
        {


            if (damageEntity != player) {
                damageEntity.playSound(damageEntity, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 10F, 1F)
                player.playSound(player, Sound.ENTITY_ARROW_HIT_PLAYER, 10F, 1F)

                (player.inventory.getItem(1)?.amount)?.let { SLASH_STACK_COUNT ->
                    HealPlayerUseCase(player, SLASH_HEAL_AMOUNT)

                    damageEntity.damage(
                            SLASH_BASE_DAMAGE_AMOUNT + (SLASH_ADD_DAMAGE_AMOUNT * (SLASH_STACK_COUNT - 1)))

                    val velocity = player.location.toVector()
                            .subtract(damageEntity.location.toVector()).multiply(-1).normalize()

                    damageEntity.velocity = velocity
                            .setY(velocity.y.coerceAtMost(SLASH_KNOCKBACK_MAX_Y).coerceAtLeast(SLASH_KNOCKBACK_MIN_Y))
                            .normalize().multiply(SLASH_KNOCKBACK_STRENGTH)
                }
            }
        }

        player.velocity = player.eyeLocation.direction.setY(0)
                .normalize().setY(SLASH_JUMP_Y).normalize().multiply(SLASH_JUMP_STRENGTH)

        player.inventory.setItem(1, ItemStack(Material.NETHER_STAR, 1))

        slashCooldown.addCooldownToPlayer(player.uniqueId, SLASH_ATTACK_COOLDOWN)
        player.setCooldown(Material.NETHER_STAR, (SLASH_ATTACK_COOLDOWN/50).toInt())

    }
/*
        player.world.spawn(player.location.add(0.0,0.99,0.0), AreaEffectCloud::class.java).let { cloud ->
            cloud.ownerUniqueId = player.uniqueId
            cloud.duration = 3
            cloud.waitTime = 3
            cloud.radius = SLASH_ATTACK_RADIUS
            cloud.addCustomEffect(PotionEffect(PotionEffectType.HARM, 0,1, false, false, false), false)
            cloud.customName(Component.text((SLASH_ATTACK_NAME)))
        }

    } */
}