package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.fighters.DefaultFighterHandler
import me.nikodemos612.classfight.utill.BounceProjectileOnHitUseCase
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
import java.util.UUID


//base = 0.2
private const val PLAYER_BASE_WALKSPEED = 0.15F
private const val PLAYER_ADD_WALKSPEED =  0.007F

private const val BALL_PROJECTILE_NAME = "ballerBall"
private const val BALL_SHOOT_COOLDOWN = 200L
private const val BALL_RELOAD_COOLDOWN = 3500L
private const val BALL_PROJECTILE_SPEED = 1.0F
private const val BALL_AMMO_COUNT = 3
private const val BALL_BASE_DAMAGE_AMOUNT = 3.0
private const val BALL_ADD_DAMAGE_AMOUNT = 1.4
private const val BALL_BASE_HEAL_AMOUNT = 3.0
private const val BALL_ADD_HEAL_AMOUNT = 0.7
private const val BALL_BOUNCE_FRICTION = 1.01
private const val BALL_BOUNCE_LIMIT = 5

private const val SUPER_BALL_PROJECTILE_NAME = "ballerSuperBall"
private const val SUPER_BALL_RELOAD_COOLDOWN = 10000L
private const val SUPER_BALL_PROJECTILE_SPEED = 2.0F
private const val SUPER_BALL_BASE_DAMAGE_AMOUNT = 2.0
private const val SUPER_BALL_ADD_DAMAGE_AMOUNT = 4.0
private const val SUPER_BALL_BOUNCE_FRICTION = 1.05
private const val SUPER_BALL_BOUNCE_LIMIT = 15

private const val BALL_EXPLOSION_COOLDOWN = 15000L
private const val BALL_EXPLOSION_DAMAGE = 5.0
private const val BALL_EXPLOSION_RADIUS = 2.0
private const val BALL_EXPLOSION_PARTICLE_AMOUNT = 300


/**
 * This class handles the BallerFighter and all it's events.
 * @author Gumend3s (Gustavo Mendes)
 * @see Cooldown
 * @see DefaultFighterHandler
 * @see BounceProjectileOnHitUseCase
 */
class BallerFighterHandler (private val plugin: Plugin) : DefaultFighterHandler() {

    var ballCount = 0
    var flyingBallCount = 0

    private val ballShotCooldown = Cooldown()
    private val ballCooldown = Cooldown()
    private val explosionCooldown = Cooldown()

    override val fighterTeamName = "baller"
    override val walkSpeed = PLAYER_BASE_WALKSPEED

    private val projectileBounceCounter = HashMap<UUID, Int>()


    override fun resetInventory(player: Player) {
        player.inventory.clear()
        player.inventory.setItem(0, ItemStack(Material.STICK))
        player.inventory.setItem(1, ItemStack(Material.SLIME_BALL, BALL_AMMO_COUNT))
        player.inventory.setItem(2, ItemStack(Material.SNOWBALL))
        player.inventory.heldItemSlot = 0

        ballCount = BALL_AMMO_COUNT

        player.allowFlight = true
        player.flySpeed = 0F

        if (player.gameMode == GameMode.CREATIVE) {
            player.flySpeed = 0.1F
        }
    }

    override fun resetCooldowns(player: Player) {
        val playerUUID = player.uniqueId

        ballShotCooldown.resetCooldown(playerUUID)
        ballCooldown.resetCooldown(playerUUID)
        explosionCooldown.resetCooldown(playerUUID)
        Bukkit.getServer().scheduler.cancelTasks(plugin)

        player.resetCooldown()
    }

    override fun onItemHeldChange(event: PlayerItemHeldEvent) {
        val player = event.player

        when (event.newSlot) {
            0 -> {

            }

            1 -> {
                player.inventory.getItem(1)?.amount.let {
                    if (it == BALL_AMMO_COUNT) {
                        shootSuperBall(player)
                    }
                }
                player.inventory.heldItemSlot = 0
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
            EntityType.SNOWBALL -> {
                when (projectile.customName()) {
                    Component.text(BALL_PROJECTILE_NAME) -> {
                        if (event.hitEntity == null) {
                            projectileBounceCounter[projectile.uniqueId]?.let {ballBounceCount ->
                                if (ballBounceCount < BALL_BOUNCE_LIMIT) {
                                    projectileBounceCounter.remove(projectile.uniqueId)
                                    BounceProjectileOnHitUseCase(event, BALL_BOUNCE_FRICTION)?.let {
                                        projectileBounceCounter[it.uniqueId] = ballBounceCount + 1
                                    }
                                } else {
                                    flyingBallCount--
                                    plugin.logger.info(flyingBallCount.toString())
                                    if (flyingBallCount == 0) {
                                        (projectile.shooter as? Player)?.let { shooter ->
                                            shooter.inventory.setItem(2, ItemStack(Material.SNOWBALL, 1))
                                        }
                                    } else {}
                                }
                            }
                        } else {
                            flyingBallCount--
                            plugin.logger.info(flyingBallCount.toString())
                            if (flyingBallCount == 0) {
                                (projectile.shooter as? Player)?.let { shooter ->
                                    shooter.inventory.setItem(2, ItemStack(Material.SNOWBALL, 1))
                                }
                            }
                        }
                    }

                    Component.text(SUPER_BALL_PROJECTILE_NAME) -> {
                        if (event.hitEntity == null) {
                            projectileBounceCounter[projectile.uniqueId]?.let { ballBounceCount ->
                                if (ballBounceCount < SUPER_BALL_BOUNCE_LIMIT) {
                                    projectileBounceCounter.remove(projectile.uniqueId)
                                    BounceProjectileOnHitUseCase(event, SUPER_BALL_BOUNCE_FRICTION)?.let {
                                        projectileBounceCounter[it.uniqueId] = ballBounceCount + 1
                                    }
                                } else {
                                    flyingBallCount--
                                    if (flyingBallCount == 0) {
                                        (projectile.shooter as? Player)?.let { shooter ->
                                            shooter.inventory.setItem(2, ItemStack(Material.SNOWBALL, 1))
                                        }
                                    } else {}
                                }
                            }
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
                        (damager.shooter as? Player)?.let {
                            val shooter = it
                            when (damager.customName()) {
                                Component.text(BALL_PROJECTILE_NAME) -> {
                                    (event.entity as? Player)?.let { entity ->
                                        projectileBounceCounter[damager.uniqueId]?.let{ ballBounceCount ->
                                            if (shooter == entity) {
                                                HealPlayerUseCase(shooter, BALL_BASE_HEAL_AMOUNT + (BALL_ADD_HEAL_AMOUNT * ballBounceCount))
                                                shooter.walkSpeed = PLAYER_BASE_WALKSPEED + ((20 - shooter.health.toFloat()) * PLAYER_ADD_WALKSPEED)
                                            } else {
                                                event.damage = BALL_BASE_DAMAGE_AMOUNT + (BALL_ADD_DAMAGE_AMOUNT * ballBounceCount)
                                            }
                                        }
                                    }
                                }

                                Component.text(SUPER_BALL_PROJECTILE_NAME) -> {
                                    (event.entity as? Player)?.let { entity ->
                                        projectileBounceCounter[damager.uniqueId]?.let{ ballBounceCount ->
                                            if (shooter != entity) {
                                                event.damage = SUPER_BALL_BASE_DAMAGE_AMOUNT + (SUPER_BALL_ADD_DAMAGE_AMOUNT * ballBounceCount)
                                            }
                                        }
                                    }
                                }

                                else -> {}
                            }
                        }
                    }

                    is AreaEffectCloud -> {
                        when (damager.customName()) {


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
        if (!ballShotCooldown.hasCooldown(player.uniqueId) && ballCount > 0) {
            when (player.inventory.getItem(1)?.type) {
                Material.SLIME_BALL -> {
                    shootBall(player)
                }
                else -> {}
            }
        }
    }

    private fun handleRightClick(player: Player) {
        if (!explosionCooldown.hasCooldown(player.uniqueId)) {
            when (player.inventory.getItem(2)?.type) {
                Material.FIRE_CHARGE -> {
                    detonateBalls(player)
                }
                else -> {}
            }
        }
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player

        player.walkSpeed = PLAYER_BASE_WALKSPEED + ((20 - player.health.toFloat()) * PLAYER_ADD_WALKSPEED)

        if (player.isFlying) {
            player.allowFlight = false
            player.flySpeed = 0.5F
        }
    }


    /**
     * Responsible shooting the ball
     *
     * @param Player The player that is shooting the shotgun
     */
    private fun shootBall(player: Player) {
        player.launchProjectile(Snowball::class.java, player.location.direction.multiply(BALL_PROJECTILE_SPEED)).let{
            it.shooter = player
            it.customName(Component.text(BALL_PROJECTILE_NAME))
            it.setGravity(true)
            projectileBounceCounter[it.uniqueId] = 0
        }

        ballShotCooldown.addCooldownToPlayer(player.uniqueId, BALL_SHOOT_COOLDOWN)
        player.setCooldown(player.inventory.getItem(0)?.type ?: Material.BEDROCK, (BALL_SHOOT_COOLDOWN/50).toInt())

        player.inventory.removeItem(ItemStack(Material.SLIME_BALL, 1))
        ballCount--

        player.inventory.getItem(1)?.type.let {
            if (it != Material.SLIME_BALL) {
                player.inventory.setItem(1, ItemStack(Material.MAGMA_CREAM, 1))
            }
        }

        flyingBallCount++
        player.inventory.setItem(2, ItemStack(Material.FIRE_CHARGE, 1))
        manageBallCooldown(player)


        plugin.logger.info(flyingBallCount.toString())
    }

    /**
     * Responsible shooting the super ball
     *
     * @param Player The player that is shooting the shotgun
     */
    private fun shootSuperBall(player: Player) {
        player.launchProjectile(Snowball::class.java, player.location.direction.multiply(SUPER_BALL_PROJECTILE_SPEED)).let{
            it.shooter = player
            it.customName(Component.text(SUPER_BALL_PROJECTILE_NAME))
            it.setGravity(false)
            projectileBounceCounter[it.uniqueId] = 0
        }

        ballCooldown.addCooldownToPlayer(player.uniqueId, SUPER_BALL_RELOAD_COOLDOWN)
        player.setCooldown(Material.SLIME_BALL, (SUPER_BALL_RELOAD_COOLDOWN/50).toInt())
        player.setCooldown(Material.MAGMA_CREAM, (SUPER_BALL_RELOAD_COOLDOWN/50).toInt())

        player.inventory.removeItem(ItemStack(Material.SLIME_BALL, 3))
        ballCount -= 3

        player.inventory.setItem(1, ItemStack(Material.MAGMA_CREAM, 1))

        Bukkit.getServer().scheduler.runTaskLater(plugin, addBall(player), SUPER_BALL_RELOAD_COOLDOWN/50)
    }

    /**
     * Responsible for managing the cooldown of the ball ammo, calls the addBall function when the ammo isn't maxed and
     * does nothing when it is
     *
     * @see addBall
     *
     * @param Player The player that is shooting the shotgun
     */
    private fun manageBallCooldown(player: Player) {
        if (!ballCooldown.hasCooldown(player.uniqueId) && ballCount < BALL_AMMO_COUNT) {
            ballCooldown.addCooldownToPlayer(player.uniqueId, BALL_RELOAD_COOLDOWN)
            player.setCooldown(Material.SLIME_BALL, (BALL_RELOAD_COOLDOWN/50).toInt())
            player.setCooldown(Material.MAGMA_CREAM, (BALL_RELOAD_COOLDOWN/50).toInt())

            Bukkit.getServer().scheduler.runTaskLater(plugin, addBall(player), BALL_RELOAD_COOLDOWN/50)
        }
    }

    /**
     * Responsible for adding ball ammo for the player and after the ball cooldown reload calls the manageBallCoowdown
     * function to continue the cycle
     *
     * @see manageBallCooldown
     *
     * @param Player The player that is shooting the shotgun
     */
    private fun addBall(player: Player) = Runnable {
        when (player.inventory.getItem(1)?.type) {
            Material.SLIME_BALL -> {
                player.inventory.getItem(1)?.amount?.let {
                    ballCount = it
                }
            }

            else -> {}
        }

        if (ballCount == 0) {
            player.inventory.setItem(1, ItemStack(Material.SLIME_BALL, 1))
        } else {
            player.inventory.addItem(ItemStack(Material.SLIME_BALL, 1))
        }
        ballCount++
        ballCooldown.resetCooldown(player.uniqueId)
        manageBallCooldown(player)
    }

    /**
     * Explodes all the regular balls still active and causes an explosion on a small area around it
     *
     * @param Player The player that is shooting the shotgun
     */
    private fun detonateBalls(player: Player) {
        for (entity in player.world.entities) {
            if (entity.customName() == Component.text(BALL_PROJECTILE_NAME) || entity.customName() == Component.text(SUPER_BALL_PROJECTILE_NAME) && entity is Snowball) {
                for (damageEntity in entity.world.getNearbyEntities(entity.location, BALL_EXPLOSION_RADIUS * 2, BALL_EXPLOSION_RADIUS * 2, BALL_EXPLOSION_RADIUS * 2)) {

                    (BALL_EXPLOSION_RADIUS / 3).let {
                        entity.world.spawnParticle(
                                Particle.DUST_PLUME,
                                entity.location,
                                BALL_EXPLOSION_PARTICLE_AMOUNT,
                                it,
                                it,
                                it,
                                0.0,
                        )
                    }

                    (damageEntity as? Player)?.let { damagePlayer ->
                        if (damagePlayer != player) {
                            damagePlayer.damage(BALL_EXPLOSION_DAMAGE)
                        }
                    }
                    entity.remove()
                }
                (entity as? Projectile)?.let {
                    flyingBallCount = 0
                    (it.shooter as? Player)?.let { shooter ->
                        shooter.inventory.setItem(2, ItemStack(Material.SNOWBALL, 1))
                    }
                }
            }
        }
        explosionCooldown.addCooldownToPlayer(player.uniqueId, BALL_EXPLOSION_COOLDOWN)
        player.setCooldown(Material.FIRE_CHARGE, (BALL_EXPLOSION_COOLDOWN/50).toInt())
        player.setCooldown(Material.SNOWBALL, (BALL_EXPLOSION_COOLDOWN/50).toInt())
    }
}