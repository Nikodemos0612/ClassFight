package me.nikodemos612.classfight.fighters.handlers
import me.nikodemos612.classfight.utill.player.Cooldown
import net.kyori.adventure.text.Component
import org.bukkit.event.player.PlayerItemHeldEvent


import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.entity.Arrow
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

private const val TEAM_NAME = "shotgunner"

private const val SHOTGUN_SHOT_COOLDOWN: Long = 5250
private const val SHOTGUN_MINI_COOLDOWN: Long = 4000
private const val SHOTGUN_PROJECTILE_DURATION: Long = 3
private const val SHOTGUN_PROJECTILE_AMOUNT: Long = 13
private const val SHOTGUN_PROJECTILE_SPEED: Float = 10F
private const val SHOTGUN_PROJECTILE_SPREAD: Float = 18F
private const val SHOTGUN_PROJECTILE_NAME = "normalShot"
private const val SHOTGUN_PROJECTILE_DAMAGE: Double = 1.0

private const val PISTOL_SHOT_COOLDOWN: Long = 3500
private const val PISTOL_PROJECTILE_SPEED: Float = 5F
private const val PISTOL_PROJECTILE_NAME = "normalShot"
private const val PISTOL_PROJECTILE_DAMAGE: Double = 7.0
private const val PISTOL_HEAL_EFFECT_DURATION = 10


class ShotgunnerFighterHandler(private val plugin: Plugin) : DefaultFighterHandler {

    private val shotgunCooldown = Cooldown()
    private val pistolCooldown = Cooldown()

    override fun canHandle(teamName: String): Boolean = teamName == TEAM_NAME

    override fun resetInventory(player: Player) {
        player.inventory.clear()
        player.inventory.setItem(0, ItemStack(Material.STICK))
        player.inventory.setItem(1, ItemStack(Material.BLAZE_ROD))
    }

    override fun resetCooldowns(player: Player) {
        val playerUUID = player.uniqueId

        shotgunCooldown.resetCooldown(playerUUID)
        pistolCooldown.resetCooldown(playerUUID)
        player.resetCooldown()
    }

    override fun onItemHeldChange(event: PlayerItemHeldEvent) {}
    override fun onPlayerInteraction(event: PlayerInteractEvent) {
        val player = event.player

        if (event.action.isLeftClick && !shotgunCooldown.hasCooldown(player.uniqueId)) {
            shootShotgun(player)
            Bukkit.getServer().scheduler.runTaskLater(plugin, endShotgunShot(player), SHOTGUN_PROJECTILE_DURATION)
        }

        if (event.action.isRightClick && !pistolCooldown.hasCooldown(player.uniqueId)) {
            shootPistol(player)
        }
    }

    override fun onProjectileHit(event: ProjectileHitEvent) {}
    override fun onPlayerHitByEntityFromThisTeam(event: EntityDamageByEntityEvent) {
        (event.damager as? Projectile)?.let { projectile ->
            when (projectile.customName()) {
                Component.text(SHOTGUN_PROJECTILE_NAME) -> {
                    event.damage = SHOTGUN_PROJECTILE_DAMAGE
                }

                Component.text(PISTOL_PROJECTILE_NAME) -> {
                    event.damage = PISTOL_PROJECTILE_DAMAGE
                    (projectile.shooter as? Player)?.addPotionEffect(
                            PotionEffect(
                                    PotionEffectType.HEAL,
                                    PISTOL_HEAL_EFFECT_DURATION,
                                    5,
                                    true,
                                    true,
                                    true,
                            ),
                    )
                }

                else -> {}
            }

            plugin.logger.info(event.damage.toString())
        }
    }

    private fun shootShotgun(player: Player) {
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
        player.setCooldown(Material.STICK, (SHOTGUN_SHOT_COOLDOWN/50).toInt())
    }

    private fun endShotgunShot(player: Player) = Runnable {
        for (entity in player.world.entities) {
            if (entity is Arrow) {
                val shooter = entity.shooter

                if (shooter is Player && shooter.uniqueId == player.uniqueId) {
                    entity.remove()
                }
            }
        }
    }

    private fun shootPistol(player: Player) {
        player.launchProjectile(Arrow::class.java, player.location.direction.multiply(PISTOL_PROJECTILE_SPEED)).let{
            it.shooter = player
            it.customName(Component.text(PISTOL_PROJECTILE_NAME))
            it.setGravity(false)
        }

        pistolCooldown.addCooldownToPlayer(player.uniqueId, PISTOL_SHOT_COOLDOWN)
        player.setCooldown(Material.BLAZE_ROD, (PISTOL_SHOT_COOLDOWN/50).toInt())
        if (!shotgunCooldown.hasCooldown(player.uniqueId)) {
            shotgunCooldown.addCooldownToPlayer(player.uniqueId, SHOTGUN_MINI_COOLDOWN)
            player.setCooldown(Material.STICK, (SHOTGUN_MINI_COOLDOWN/50).toInt())
        }
    }

}