package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.fighters.DefaultFighterHandler
import me.nikodemos612.classfight.utill.BounceProjectileOnHitUseCase
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


private val TEAM_NAME = "banger"

class BangerFighterHandler(private val plugin: Plugin) : DefaultFighterHandler {
    override fun canHandle(teamName: String): Boolean = teamName == TEAM_NAME

    override fun onItemHeldChange(event: PlayerItemHeldEvent) {}

    override fun onPlayerMove(event: PlayerMoveEvent) {}

    override fun onInventoryClick(event: InventoryClickEvent) {}

    override fun onPlayerInteraction(event: PlayerInteractEvent) {

        if (event.action.isLeftClick) {
            val player = event.player

            player.launchProjectile(Snowball::class.java, player.location.direction)

            Bukkit.getServer().scheduler.runTaskLater(plugin, explodeFlashBangProjectileFromPlayer(player), 40L)
        }
    }

    override fun onProjectileHit(event: ProjectileHitEvent) {
        BounceProjectileOnHitUseCase(event)
    }

    private fun explodeFlashBangProjectileFromPlayer(player: Player) = Runnable {
        for (entity in player.world.entities) {
            if (entity is Snowball) {
                val shooter = entity.shooter

                if (shooter is Player && shooter.uniqueId == player.uniqueId) {
                    flashBangPlayersAround(entity)
                    entity.remove()
                }
            }
        }
    }

    private fun flashBangPlayersAround(entity: Snowball){
        val playersToVerify = entity.location.getNearbyPlayers(200.0)

        for (player in playersToVerify) {
            if (player.hasLineOfSight(entity) && hasFlashBangInCone(
                flashBang = entity,
                startpoint = player.location,
                radius = 200,
                degrees = 120,
                direction = player.bodyYaw.toInt()
            ))
                player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 1000, 1))
        }
    }

    // Codigo da interwebs =================================================

    /**
     * Gets entities inside a cone.
     * @see Utilities.getPlayersInCone
     * @param entities - `List<Entity>`, list of nearby entities
     * @param startpoint - `Location`, center point
     * @param radius - `int`, radius of the circle
     * @param degrees - `int`, angle of the cone
     * @param direction - `int`, direction of the cone
     * @return `List<Entity>` - entities in the cone
     */
    private fun hasFlashBangInCone(
        flashBang: Snowball,
        startpoint: Location,
        radius: Int,
        degrees: Int,
        direction: Int,
    ): Boolean {

        val startPos = intArrayOf(startpoint.x.toInt(), startpoint.z.toInt())

        val endA = intArrayOf(
            (radius * cos((direction - (degrees / 2)).toDouble())).toInt(),
            (radius * sin((direction - (degrees / 2)).toDouble())).toInt()
        )

        val location: Location = flashBang.location
        val entityVector = getVectorForPoints(startPos[0], startPos[1], location.blockX, location.blockY)

        val angle = getAngleBetweenVectors(endA, entityVector)
        return Math.toDegrees(angle) < degrees && Math.toDegrees(angle) > 0
    }

    /**
     * Created an integer vector in 2d between two points
     *
     * @param x1 - `int`, X pos 1
     * @param y1 - `int`, Y pos 1
     * @param x2 - `int`, X pos 2
     * @param y2 - `int`, Y pos 2
     * @return `int[]` - vector
     */
    private fun getVectorForPoints(x1: Int, y1: Int, x2: Int, y2: Int): IntArray {
        return intArrayOf(x2 - x1, y2 - y1)
    }

    /**
     * Get the angle between two vectors.
     *
     * @param vector1 - `int[]`, vector 1
     * @param vector2 - `int[]`, vector 2
     * @return `double` - angle
     */
    private fun getAngleBetweenVectors(vector1: IntArray, vector2: IntArray): Double {
        return atan2(vector2[1].toDouble(), vector2[0].toDouble()) - atan2(vector1[1].toDouble(), vector1[0].toDouble())
    }
}