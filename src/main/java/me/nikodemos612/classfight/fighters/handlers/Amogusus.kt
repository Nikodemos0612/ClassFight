package me.nikodemos612.classfight.fighters.handlers
import org.bukkit.event.player.PlayerItemHeldEvent


import me.nikodemos612.classfight.fighters.DefaultFighterHandler
import org.bukkit.Material
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import java.util.*

private const val TEAM_NAME = "amogusus"

private const val INITIAL_SHOT_COOLDOWN: Long = 1200
private const val SHOTGUN_PROJECTILE_DURATION: Long = 350
private const val SHOTGUN_PROJECTILE_AMOUNT: Long = 6
private const val SHOTGUN_PROJECTILE_SPEED: Float = 3F
private const val SHOTGUN_PROJECTILE_SPREAD: Float = 7F


object amogusus :DefaultFighterHandler{

    private val shotCooldown = HashMap<UUID, Long>()

    override fun canHandle(teamName: String) = teamName == TEAM_NAME
    override fun onItemHeldChange(event: PlayerItemHeldEvent) {}
    override fun onPlayerMove(event: PlayerMoveEvent) {}
    override fun onInventoryClick(event: InventoryClickEvent) {}
    override fun onPlayerInteraction(event: PlayerInteractEvent) {
        val player = event.player

        if (event.action.isLeftClick && !hasCooldown(shotCooldown, player.uniqueId)) {
            shootShotGun(player)
        }

    }

    private fun hasCooldown(cooldownToVerify : HashMap<UUID, Long>, player: UUID) : Boolean {
        val cooldown = cooldownToVerify[player]
        if(cooldown != null && cooldown < System.currentTimeMillis() ) {
            cooldownToVerify.remove(player)
        }

        return cooldownToVerify[player] != null
    }


    private fun shootShotGun(player: Player) {
        for(i in 1..SHOTGUN_PROJECTILE_AMOUNT) {
            player.world.spawnArrow(player.eyeLocation, player.eyeLocation.direction, SHOTGUN_PROJECTILE_SPEED, SHOTGUN_PROJECTILE_SPREAD)
        }
        shotCooldown[player.uniqueId] = System.currentTimeMillis() + INITIAL_SHOT_COOLDOWN
        player.setCooldown(Material.STONE_SWORD, (INITIAL_SHOT_COOLDOWN/50).toInt())
    }
}