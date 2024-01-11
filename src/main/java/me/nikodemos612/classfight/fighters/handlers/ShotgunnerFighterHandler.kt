package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.utill.player.Cooldown
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
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

private const val TEAM_NAME = "shotgunner"

private const val SHOTGUN_SHOT_COOLDOWN: Long = 0//7500
private const val SHOTGUN_PROJECTILE_DURATION: Long = 3
private const val SHOTGUN_PROJECTILE_SPEED: Float = 10F
private const val SHOTGUN_PROJECTILE_NAME = "shotgunShot"
private const val SHOTGUN_PROJECTILE_DAMAGE: Double = 1.0
private const val SHOTGUN_PROJECTILE_AMOUNT = 16
private const val SHOTGUN_PROJECTILE_SPREAD = 20F
private const val SHOTGUN_DASH_Y: Double = 0.5
private const val SHOTGUN_DASH_STRENGHT: Double = 1.0
private const val SHOTGUN_DASH_COOLDOWN: Long = 7000

private const val SHOTGUN_MINI_BASE_AMMO: Int = 1
private const val SHOTGUN_MINI_ADD_AMMO: Int = 1
private const val SHOTGUN_MINI_BASE_COOLDOWN: Long = 200
private const val SHOTGUN_MINI_ADD_COOLDOWN_RATIO: Float = 1.5F
private const val SHOTGUN_MINI_PROJECTILE_DURATION: Long = 4
private const val SHOTGUN_MINI_PROJECTILE_SPEED: Float = 10F
private const val SHOTGUN_MINI_PROJECTILE_AMOUNT = 8
private const val SHOTGUN_MINI_PROJECTILE_SPREAD = 10F
private const val SHOTGUN_MINI_DASH_Y: Double = 10.0
private const val SHOTGUN_MINI_DASH_STRENGHT: Double = 0.8
private const val SHOTGUN_MINI_DASH_COOLDOWN: Long = 4500

private const val PISTOL_SHOT_COOLDOWN: Long = 11000
private const val PISTOL_PROJECTILE_SPEED: Float = 2F
private const val PISTOL_PROJECTILE_NAME = "pistolShot"
private const val PISTOL_PROJECTILE_DAMAGE: Double = 8.0
private const val PISTOL_HEAL_EFFECT_STRENGHT = 12.0
private const val PISTOL_PULL_STRENGHT = 0.4


class ShotgunnerFighterHandler(private val plugin: Plugin) : DefaultFighterHandler() {

    private val shotgunCooldown = Cooldown()
    private val pistolCooldown = Cooldown()
    private val dashCooldown = Cooldown()

    override fun canHandle(teamName: String): Boolean = teamName == TEAM_NAME

    override fun resetInventory(player: Player) {
        player.inventory.clear()
        player.inventory.setItem(0, ItemStack(Material.STICK))
        player.inventory.setItem(1, ItemStack(Material.TRIPWIRE_HOOK))
        player.inventory.setItem(2, ItemStack(Material.ENDER_PEARL))
        player.inventory.heldItemSlot = 0

        player.allowFlight = true
        player.flySpeed = 0F
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
                        if (shooter.health + PISTOL_HEAL_EFFECT_STRENGHT < 20) {
                            shooter.health += PISTOL_HEAL_EFFECT_STRENGHT
                        } else {
                            shooter.health = 20.0
                        }
                        val velocity = shooter.location.toVector().subtract(event.entity.location.toVector())
                        event.entity.velocity = velocity.setY(velocity.y.coerceAtLeast(0.25))
                            .multiply(PISTOL_PULL_STRENGHT)
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

    private fun shootPistol(player: Player) {
        player.launchProjectile(Arrow::class.java, player.location.direction.multiply(PISTOL_PROJECTILE_SPEED)).let{
            it.shooter = player
            it.customName(Component.text(PISTOL_PROJECTILE_NAME))
            it.setGravity(false)
        }

        pistolCooldown.addCooldownToPlayer(player.uniqueId, PISTOL_SHOT_COOLDOWN)
        player.setCooldown(Material.TRIPWIRE_HOOK, (PISTOL_SHOT_COOLDOWN/50).toInt())
    }

    private fun horizontalDash(player: Player) {
        player.velocity = player.eyeLocation.direction.setY(0).normalize().setY(SHOTGUN_DASH_Y).normalize().multiply(SHOTGUN_DASH_STRENGHT)
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

    private fun verticalDash(player: Player) {
        player.velocity = player.eyeLocation.direction.setY(0).normalize().setY(SHOTGUN_MINI_DASH_Y).normalize().multiply(SHOTGUN_MINI_DASH_STRENGHT)
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