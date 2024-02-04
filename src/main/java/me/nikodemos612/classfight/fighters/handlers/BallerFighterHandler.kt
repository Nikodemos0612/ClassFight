package me.nikodemos612.classfight.fighters.handlers

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
private const val BALL_RELOAD_COOLDOWN = 3000L
private const val BALL_PROJECTILE_SPEED = 1.0F
private const val BALL_AMMO_COUNT = 3
private const val BALL_BASE_DAMAGE_AMOUNT = 3.0
private const val BALL_ADD_DAMAGE_AMOUNT = 1.4
private const val BALL_BASE_HEAL_AMOUNT = 3.0
private const val BALL_ADD_HEAL_AMOUNT = 0.7
private const val BALL_BOUNCE_FRICTION = 1.01
private const val BALL_BOUNCE_LIMIT = 5

private const val BALL_EXPLOSION_COOLDOWN = 10000L
private const val BALL_EXPLOSION_DAMAGE = 5.0
private const val BALL_EXPLOSION_RADIUS = 2.0
private const val BALL_EXPLOSION_PARTICLE_AMOUNT = 300

class BallerFighterHandler (private val plugin: Plugin) : DefaultFighterHandler() {

    var ballCount = 0

    private val ballShotCooldown = Cooldown()
    private val ballCooldown = Cooldown()
    private val explosionCooldown = Cooldown()

    override val fighterTeamName = "Baller"
    override val walkSpeed = PLAYER_BASE_WALKSPEED

    private val projectileBounceCounter = HashMap<UUID, Int>()


    override fun resetInventory(player: Player) {
        player.inventory.clear()
        player.inventory.setItem(0, ItemStack(Material.STICK))
        player.inventory.setItem(1, ItemStack(Material.SLIME_BALL, BALL_AMMO_COUNT))
        player.inventory.setItem(2, ItemStack(Material.FIRE_CHARGE))
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

            else -> {
                player.inventory.heldItemSlot = 0
            }
        }
    }

    override fun onPlayerInteraction(event: PlayerInteractEvent) {
        val player = event.player

        if (event.action.isLeftClick && !ballShotCooldown.hasCooldown(player.uniqueId) && ballCount > 0) {
            when (player.inventory.getItem(1)?.type) {
                Material.SLIME_BALL -> {
                    shootBall(player)
                }
                else -> {}
            }
        }

        if (event.action.isRightClick && !explosionCooldown.hasCooldown(player.uniqueId)) {
            detonateBalls(player)
        }
    }

    override fun onProjectileHit(event: ProjectileHitEvent) {

        val projectile = event.entity as? Projectile
        when (projectile?.type) {
            EntityType.SNOWBALL -> {
                when (projectile.customName()) {
                    Component.text(BALL_PROJECTILE_NAME) -> {
                        if (event.hitEntity != null) { } else {
                            projectileBounceCounter[projectile.uniqueId]?.let {ballBounceCount ->
                                if (ballBounceCount < BALL_BOUNCE_LIMIT) {
                                    projectileBounceCounter.remove(projectile.uniqueId)
                                    BounceProjectileOnHitUseCase(event, BALL_BOUNCE_FRICTION)?.let {
                                        projectileBounceCounter[it.uniqueId] = ballBounceCount + 1
                                    }
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
        plugin.logger.info(event.damage.toString())
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player

        player.walkSpeed = PLAYER_BASE_WALKSPEED + ((20 - player.health.toFloat()) * PLAYER_ADD_WALKSPEED)

        if (player.isFlying) {
            player.allowFlight = false
            player.flySpeed = 0.5F
        }
    }

    override fun onPlayerDamage(event: EntityDamageEvent) {

    }


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

        manageBallCooldown(player)
    }

    private fun manageBallCooldown(player: Player) {
        if (!ballCooldown.hasCooldown(player.uniqueId) && ballCount < BALL_AMMO_COUNT) {
            ballCooldown.addCooldownToPlayer(player.uniqueId, BALL_RELOAD_COOLDOWN)
            player.setCooldown(Material.SLIME_BALL, (BALL_RELOAD_COOLDOWN/50).toInt())
            player.setCooldown(Material.MAGMA_CREAM, (BALL_RELOAD_COOLDOWN/50).toInt())

            Bukkit.getServer().scheduler.runTaskLater(plugin, addBall(player), BALL_RELOAD_COOLDOWN/50)
        }
    }

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

    private fun detonateBalls(player: Player) {
        for (entity in player.world.entities) {
            if (entity.customName() == Component.text(BALL_PROJECTILE_NAME) && entity is Snowball) {
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
            }
        }
        explosionCooldown.addCooldownToPlayer(player.uniqueId, BALL_EXPLOSION_COOLDOWN)
        player.setCooldown(Material.FIRE_CHARGE, (BALL_EXPLOSION_COOLDOWN/50).toInt())
    }
}