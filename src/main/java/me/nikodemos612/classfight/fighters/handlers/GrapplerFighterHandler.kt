package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.utill.player.Cooldown
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
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


private const val GRAPPLE_SHOT_COOLDOWN: Long = 3000
private const val GRAPPLE_PROJECTILE_SPEED: Float = 3F
private const val GRAPPLE_PROJECTILE_NAME = "grappleShot"
private const val GRAPPLE_PROJECTILE_DAMAGE: Double = 6.0
private const val GRAPPLE_PULL_STRENGHT = 1.5

private const val DOUBLE_JUMP_COOLDOWN: Long = 3000
private const val DOUBLE_JUMP_STRENGHT: Double = 1.0
private const val DOUBLE_JUMP_Y: Double = 1.1

private const val TEAM_NAME = "grappler"

class GrapplerFighterHandler(private val plugin: Plugin) : DefaultFighterHandler  {

    private val grappleCooldown = Cooldown()
    
    override fun canHandle(teamName: String): Boolean = teamName == TEAM_NAME

    override fun resetInventory(player: Player) {
        player.inventory.clear()
        player.inventory.setItem(0, ItemStack(Material.STICK))
        player.inventory.setItem(1, ItemStack(Material.RABBIT_FOOT))
        player.inventory.heldItemSlot = 0

        player.allowFlight = true
        player.flySpeed = 0F

        player.addPotionEffect(PotionEffect(PotionEffectType.JUMP, PotionEffect.INFINITE_DURATION, 2, false,false))
    }

    override fun resetCooldowns(player: Player) {
        val playerUUID = player.uniqueId

        grappleCooldown.resetCooldown(playerUUID)
        
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
    }

    override fun onProjectileHit(event: ProjectileHitEvent) {

        val projectile = event.entity as? Arrow
        projectile?.let {
            when (projectile.customName()) {
                Component.text(GRAPPLE_PROJECTILE_NAME) -> { pullPlayer(projectile)
                    (projectile.shooter as? Player)?.let {  shooter ->
                        shooter.inventory.setItem(0, ItemStack(Material.STICK, 1))
                        grappleCooldown.addCooldownToPlayer(shooter.uniqueId, GRAPPLE_SHOT_COOLDOWN)
                        shooter.setCooldown(Material.STICK, (GRAPPLE_SHOT_COOLDOWN/50).toInt())
                    }
                }

                else -> {}
            }
        }
        event.entity.remove()
    }

    override fun onPlayerHitByEntityFromThisTeam(event: EntityDamageByEntityEvent) {
        (event.damager as? Projectile)?.let { projectile ->
            when (projectile.customName()) {
            Component.text(GRAPPLE_PROJECTILE_NAME) -> {
                    event.damage = GRAPPLE_PROJECTILE_DAMAGE
                }

                else -> {}
            }
            plugin.logger.info(event.damage.toString())
        }
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player

        if (player.isFlying) {
            player.allowFlight = false
            player.flySpeed = 1F

            player.velocity = player.eyeLocation.direction.setY(0).normalize().setY(DOUBLE_JUMP_Y).normalize().multiply(DOUBLE_JUMP_STRENGHT)
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
            shooter.velocity = velocity.setY(velocity.y.coerceAtMost(4.0).coerceAtLeast(0.25))
                    .normalize().multiply(GRAPPLE_PULL_STRENGHT)
        }
    }
}