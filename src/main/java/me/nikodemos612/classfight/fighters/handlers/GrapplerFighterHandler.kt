package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.utill.BounceProjectileOnHitUseCase
import me.nikodemos612.classfight.utill.HealPlayerUseCase
import me.nikodemos612.classfight.utill.cooldown.Cooldown
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

//base = 0.2
private const val PLAYER_WAKSPEED = 0.3F

private const val GRAPPLE_PROJECTILE_NAME = "grappleShot"
private const val GRAPPLE_PROJECTILE_SPEED = 3F
private const val GRAPPLE_PROJECTILE_DAMAGE = 7.0
private const val GRAPPLE_SHOT_COOLDOWN = 6000L
private const val GRAPPLE_PULL_STRENGTH = 1.5
private const val GRAPPLE_PULL_MAX_Y = 2.7
private const val GRAPPLE_PULL_MIN_Y = 0.25

private const val DOUBLE_JUMP_COOLDOWN = 3500L
private const val DOUBLE_JUMP_STRENGTH = 1.0
private const val DOUBLE_JUMP_Y = 1.1

private const val SLASH_ATTACK_NAME = "grapplerSlash"
private const val SLASH_ATTACK_COOLDOWN = 4000L
private const val SLASH_ATTACK_RADIUS = 4.5F
private const val SLASH_HEAL_AMOUNT = 4.0
private const val SLASH_BASE_DAMAGE_AMOUNT = 3.0
private const val SLASH_ADD_DAMAGE_AMOUNT = 2.0
private const val SLASH_KNOCKBACK_STRENGTH = 1.8F
private const val SLASH_KNOCKBACK_MAX_Y = 3.0
private const val SLASH_KNOCKBACK_MIN_Y = 0.35
private const val SLASH_JUMP_STRENGTH = 0.8
private const val SLASH_JUMP_Y = 0.9


class GrapplerFighterHandler(private val plugin: Plugin) : DefaultFighterHandler() {

    private var SLASH_TOTAL_DAMAGE_AMOUNT = 0.0

    private val grappleCooldown = Cooldown()
    private val slashCooldown = Cooldown()

    override val fighterTeamName = "grappler"

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

        player.walkSpeed = PLAYER_WAKSPEED

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

        if (event.action.isLeftClick && !grappleCooldown.hasCooldown(player.uniqueId)) {
            when (player.inventory.getItem(0)?.type) {
                Material.STICK -> {
                    shootGrapple(player)
                }
                else -> {}
            }
        }

        if (event.action.isRightClick && !slashCooldown.hasCooldown(player.uniqueId)) {
            attackSlash(player)
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

            is AreaEffectCloud -> {
                when (damager.customName()) {
                    Component.text(SLASH_ATTACK_NAME) -> {
                        (event.entity as? Player)?.let { player ->
                            if (player.uniqueId == damager.ownerUniqueId) {
                                event.isCancelled = true
                            } else {
                                for (entity in player.world.entities) {
                                    (entity as? Player)?.let { p ->
                                        if (entity.uniqueId == damager.ownerUniqueId) {
                                            HealPlayerUseCase.invoke(entity, SLASH_HEAL_AMOUNT)
                                        }
                                    }
                                }
                                event.damage = SLASH_TOTAL_DAMAGE_AMOUNT

                                val velocity = damager.location.toVector().subtract(player.location.toVector()).multiply(-1).normalize()
                                player.velocity = velocity.setY(velocity.y.coerceAtMost(SLASH_KNOCKBACK_MAX_Y).coerceAtLeast(SLASH_KNOCKBACK_MIN_Y))
                                        .normalize().multiply(SLASH_KNOCKBACK_STRENGTH)
                            }
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
            shooter.velocity = velocity.setY(velocity.y.coerceAtMost(GRAPPLE_PULL_MAX_Y).coerceAtLeast(GRAPPLE_PULL_MIN_Y))
                    .normalize().multiply(GRAPPLE_PULL_STRENGTH)
        }
    }

    private fun attackSlash(player: Player) {
        player.world.spawn(player.location.add(0.0,0.99,0.0), AreaEffectCloud::class.java).let { cloud ->
            cloud.ownerUniqueId = player.uniqueId
            cloud.duration = 3
            cloud.waitTime = 3
            cloud.radius = SLASH_ATTACK_RADIUS
            cloud.addCustomEffect(PotionEffect(PotionEffectType.HARM, 0,1, false, false, false), false)
            cloud.customName(Component.text((SLASH_ATTACK_NAME)))
        }
        (player.inventory.getItem(1)?.amount)?.let { SLASH_STACK_COUNT ->
            SLASH_TOTAL_DAMAGE_AMOUNT = SLASH_BASE_DAMAGE_AMOUNT + (SLASH_ADD_DAMAGE_AMOUNT * (SLASH_STACK_COUNT - 1))
        }

        player.velocity = player.eyeLocation.direction.setY(0).normalize().setY(SLASH_JUMP_Y).normalize().multiply(SLASH_JUMP_STRENGTH)

        player.inventory.setItem(1, ItemStack(Material.NETHER_STAR, 1))

        slashCooldown.addCooldownToPlayer(player.uniqueId, SLASH_ATTACK_COOLDOWN)
        player.setCooldown(Material.NETHER_STAR, (SLASH_ATTACK_COOLDOWN/50).toInt())
    }
}