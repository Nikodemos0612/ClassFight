package me.nikodemos612.classfight.fighters.handlers
import org.bukkit.event.player.PlayerItemHeldEvent


import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.entity.Arrow
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin
import java.util.*

private const val TEAM_NAME = "shotgunner"

private const val INITIAL_SHOT_COOLDOWN: Long = 1200
private const val SHOTGUN_PROJECTILE_DURATION: Long = 8
private const val SHOTGUN_PROJECTILE_AMOUNT: Long = 5
private const val SHOTGUN_PROJECTILE_SPEED: Float = 3F
private const val SHOTGUN_PROJECTILE_SPREAD: Float = 4F


class ShotgunnerFighterHandler(private val plugin: Plugin) : DefaultFighterHandler {

    private val shotCooldown = HashMap<UUID, Long>()

    override fun canHandle(teamName: String): Boolean = teamName == TEAM_NAME

    override fun resetInventory(player: Player) {
        TODO("Not yet implemented")
    }

    override fun resetCooldowns(player: Player) {
        TODO("Not yet implemented")
    }

    override fun onItemHeldChange(event: PlayerItemHeldEvent) {}
    override fun onPlayerInteraction(event: PlayerInteractEvent) {
        val player = event.player

        if (event.action.isLeftClick && !hasCooldown(shotCooldown, player.uniqueId)) {
            shootShotgun(player)
            Bukkit.getServer().scheduler.runTaskLater(plugin, endShotgunShot(player), SHOTGUN_PROJECTILE_DURATION)
        }
    }

    override fun onProjectileHit(event: ProjectileHitEvent) {}

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
        shotCooldown[player.uniqueId] = System.currentTimeMillis() + INITIAL_SHOT_COOLDOWN
        player.setCooldown(Material.BRUSH, (INITIAL_SHOT_COOLDOWN/50).toInt())
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

}