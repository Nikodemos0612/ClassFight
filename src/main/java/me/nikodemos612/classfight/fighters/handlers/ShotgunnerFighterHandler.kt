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
import java.util.*

private const val TEAM_NAME = "shotgunner"

private const val SHOTGUN_SHOT_COOLDOWN: Long = 3500
private const val SHOTGUN_PROJECTILE_DURATION: Long = 4
private const val SHOTGUN_PROJECTILE_AMOUNT: Long = 10
private const val SHOTGUN_PROJECTILE_SPEED: Float = 3F
private const val SHOTGUN_PROJECTILE_SPREAD: Float = 15F
private const val SHOTGUN_PROJECTILE_NAME = "normalShot"
private const val SHOTGUN_PROJECTILE_DAMAGE: Double = 2.0

private const val PISTOL_SHOT_COOLDOWN: Long = 3500
private const val PISTOL_PROJECTILE_SPEED: Float = 5F
private const val PISTOL_PROJECTILE_NAME = "normalShot"
private const val PISTOL_MIN_PROJECTILE_DAMAGE: Double = 0.0
private const val PISTOL_MAX_PROJECTILE_DAMAGE: Double = 4.0


class ShotgunnerFighterHandler(private val plugin: Plugin) : DefaultFighterHandler {

    private val shotCooldown = Cooldown()

    override fun canHandle(teamName: String): Boolean = teamName == TEAM_NAME

    override fun resetInventory(player: Player) {
        player.inventory.clear()
        player.inventory.setItem(0, ItemStack(Material.STICK))
    }

    override fun resetCooldowns(player: Player) {
    }

    override fun onItemHeldChange(event: PlayerItemHeldEvent) {}
    override fun onPlayerInteraction(event: PlayerInteractEvent) {
        val player = event.player

        if (event.action.isLeftClick && shotCooldown.hasCooldown(player.uniqueId)) {
            shootShotgun(player)
            Bukkit.getServer().scheduler.runTaskLater(plugin, endShotgunShot(player), SHOTGUN_PROJECTILE_DURATION)
        }

        if (event.action.isRightClick && shotCooldown.hasCooldown(player.uniqueId)) {
            shootPistol(player)
            Bukkit.getServer().scheduler.runTaskLater(plugin, endShotgunShot(player), SHOTGUN_PROJECTILE_DURATION)
        }
    }

    override fun onProjectileHit(event: ProjectileHitEvent) {}
    override fun onPlayerHitByEntityFromThisTeam(event: EntityDamageByEntityEvent) {
        (event.damager as? Projectile)?.let { projectile ->
            when (projectile.customName()) {
                Component.text(SHOTGUN_PROJECTILE_NAME) -> {
                    event.damage = SHOTGUN_PROJECTILE_DAMAGE
                }
            }

            when (projectile.customName()) {
                Component.text(PISTOL_PROJECTILE_NAME) -> {
                    event.damage = PISTOL_MIN_PROJECTILE_DAMAGE
                }
            }
        }
    }

    private fun hasCooldown(cooldownToVerify : HashMap<UUID, Long>, player: UUID) : Boolean {
        val cooldown = cooldownToVerify[player]
        if(cooldown != null && cooldown < System.currentTimeMillis() ) {
            cooldownToVerify.remove(player)
        }

        return cooldownToVerify[player] != null
    }


    private fun shootShotgun(player: Player) {
        for(i in 1..SHOTGUN_PROJECTILE_AMOUNT) {
            player.world.spawnArrow(
                    player.eyeLocation,
                    player.eyeLocation.direction,
                    SHOTGUN_PROJECTILE_SPEED,
                    SHOTGUN_PROJECTILE_SPREAD
            ).shooter = player
        }
        shotCooldown.addCooldownToPlayer(player.uniqueId, SHOTGUN_SHOT_COOLDOWN)
        player.setCooldown(Material.BRUSH, (SHOTGUN_SHOT_COOLDOWN/50).toInt())
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

        shotCooldown.addCooldownToPlayer(player.uniqueId, PISTOL_SHOT_COOLDOWN)
        player.setCooldown(Material.BRUSH, (PISTOL_SHOT_COOLDOWN/50).toInt())
    }

}