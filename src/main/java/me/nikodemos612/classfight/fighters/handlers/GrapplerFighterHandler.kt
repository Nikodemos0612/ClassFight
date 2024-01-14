package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.utill.BounceProjectileOnHitUseCase
import me.nikodemos612.classfight.utill.player.Cooldown
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
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


private const val TEAM_NAME = "grappler"

private const val GRAPPLE_PROJECTILE_NAME = "grappleShot"
private const val GRAPPLE_PROJECTILE_SPEED = 3F
private const val GRAPPLE_PROJECTILE_DAMAGE = 7.0
private const val GRAPPLE_SHOT_COOLDOWN = 6000L
private const val GRAPPLE_PULL_STRENGTH = 1.5

private const val DOUBLE_JUMP_COOLDOWN = 3500L
private const val DOUBLE_JUMP_STRENGTH = 1.0
private const val DOUBLE_JUMP_Y = 1.1

private const val GRENADE_PROJECTILE_NAME = "grapplerGrenade"
private const val GRENADE_EXPLOSION_NAME = "grapplerExplosion"
private const val GRENADE_PROJECTILE_SPEED = 1.05
private const val GRENADE_PROJECTILE_FRICTION = 0.225
private const val GRENADE_SHOT_COOLDOWN = 10000L
private const val GRENADE_HEAL_AMOUNT = 8
private const val GRENADE_DAMAGE_AMOUNT = 10.0
private const val GRENADE_KNOCKBACK_STRENGTH = 1.8F
private const val GRENADE_DETONATION_TIME = 1000
private const val GRENADE_DETONATION_RADIUS = 4.5F


class GrapplerFighterHandler(private val plugin: Plugin) : DefaultFighterHandler() {

    private val grappleCooldown = Cooldown()
    private val grenadeCooldown = Cooldown()
    
    override fun canHandle(teamName: String): Boolean = teamName == TEAM_NAME

    override fun resetInventory(player: Player) {
        player.inventory.clear()
        player.inventory.setItem(0, ItemStack(Material.STICK))
        player.inventory.setItem(1, ItemStack(Material.FIREWORK_STAR))
        player.inventory.setItem(2, ItemStack(Material.RABBIT_FOOT))
        player.inventory.heldItemSlot = 0

        player.allowFlight = true
        player.flySpeed = 0F

        if (player.gameMode == GameMode.CREATIVE) {
            player.flySpeed = 1F
        }

        player.addPotionEffect(PotionEffect(PotionEffectType.JUMP, PotionEffect.INFINITE_DURATION, 2, false,false))
    }

    override fun resetCooldowns(player: Player) {
        val playerUUID = player.uniqueId

        grappleCooldown.resetCooldown(playerUUID)
        grenadeCooldown.resetCooldown(playerUUID)
        
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

        if (event.action.isLeftClick && !grappleCooldown.hasCooldown(player.uniqueId)) {
            when (player.inventory.getItem(0)?.type) {
                Material.STICK -> {
                    shootGrapple(player)
                }
                else -> {}
            }
        }

        if (event.action.isRightClick && !grenadeCooldown.hasCooldown(player.uniqueId)) {
            shootGrenade(player)
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

            EntityType.SNOWBALL -> {
                when (val entity = event.hitEntity) {
                    is Player -> {
                        entity.launchProjectile(Snowball::class.java, projectile.velocity).let{grenade ->
                            grenade.shooter = projectile.shooter
                            grenade.customName(Component.text(GRENADE_PROJECTILE_NAME))
                            grenade.setGravity(true)
                        }
                    }

                    else -> {
                        when (projectile.customName()) {
                            Component.text(GRENADE_PROJECTILE_NAME) -> {
                                BounceProjectileOnHitUseCase(event, GRENADE_PROJECTILE_FRICTION)
                            }

                            else -> {}
                        }
                    }
                }
            }

            else -> {}
        }
        event.entity.remove()
    }

    override fun onPlayerHitByEntityFromThisTeam(event: EntityDamageByEntityEvent) {
        when (val damager = event.damager) {
            is Projectile -> {
                when (damager.customName()) {
                    Component.text(GRAPPLE_PROJECTILE_NAME) -> {
                        event.damage = GRAPPLE_PROJECTILE_DAMAGE
                    }

                    else -> {}
                }
            }

            is AreaEffectCloud -> {
                when (damager.customName()) {
                    Component.text(GRENADE_EXPLOSION_NAME) -> {
                        (event.entity as? Player)?.let { player ->
                            if (player.uniqueId == damager.ownerUniqueId) {
                                event.isCancelled = true
                                if (player.health + GRENADE_HEAL_AMOUNT < 20) {
                                    player.health += GRENADE_HEAL_AMOUNT
                                } else {
                                    player.health = 20.0
                                }
                            } else {
                                event.damage = GRENADE_DAMAGE_AMOUNT
                            }
                            val velocity = damager.location.toVector().subtract(player.location.toVector()).multiply(-1).normalize()
                            player.velocity = velocity.setY(velocity.y.coerceAtMost(3.0).coerceAtLeast(0.35))
                                    .normalize().multiply(GRENADE_KNOCKBACK_STRENGTH)
                        }
                    }

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

        if (player.isFlying) {
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

    private fun resetDoubleJump(player: Player) = Runnable {
        player.allowFlight = true
        player.flySpeed = 0F
    }

    private fun shootGrapple(player: Player) {
        player.launchProjectile(Arrow::class.java, player.location.direction.multiply(GRAPPLE_PROJECTILE_SPEED)).let{
            it.shooter = player
            it.customName(Component.text(GRAPPLE_PROJECTILE_NAME))
            it.setGravity(false)
        }

        player.inventory.setItem(0, ItemStack(Material.BARRIER, 1))
    }

    private fun pullPlayer(projectile: Projectile) {
        (projectile.shooter as? Player)?.let {  shooter ->
            val velocity = projectile.location.toVector().subtract(shooter.location.toVector())
            shooter.velocity = velocity.setY(velocity.y.coerceAtMost(2.7).coerceAtLeast(0.25))
                    .normalize().multiply(GRAPPLE_PULL_STRENGTH)
        }
    }

    private fun shootGrenade(player: Player) {
        player.launchProjectile(Snowball::class.java, player.location.direction.multiply(GRENADE_PROJECTILE_SPEED)).let{grenade ->
            grenade.shooter = player
            grenade.customName(Component.text(GRENADE_PROJECTILE_NAME))
            grenade.setGravity(true)
        }

        grenadeCooldown.addCooldownToPlayer(player.uniqueId, GRENADE_SHOT_COOLDOWN)
        player.setCooldown(Material.FIREWORK_STAR, (GRENADE_SHOT_COOLDOWN/50).toInt())
        Bukkit.getServer().scheduler.runTaskLater(plugin, explodeGrenade(player), (GRENADE_DETONATION_TIME/50).toLong())
    }

    private fun explodeGrenade(player: Player) = Runnable {
        for (entity in player.world.entities) {
            if (entity.customName() == Component.text(GRENADE_PROJECTILE_NAME) && entity is Snowball) {
                entity.world.spawn(entity.location, AreaEffectCloud::class.java).let { cloud ->
                    cloud.ownerUniqueId = player.uniqueId
                    cloud.duration = 3
                    cloud.waitTime = 3
                    cloud.radius = GRENADE_DETONATION_RADIUS
                    cloud.addCustomEffect(PotionEffect(PotionEffectType.HARM, 0,1, false, false, false), false)
                    cloud.customName(Component.text((GRENADE_EXPLOSION_NAME)))
                }
                entity.remove()
            }
        }
    }
}