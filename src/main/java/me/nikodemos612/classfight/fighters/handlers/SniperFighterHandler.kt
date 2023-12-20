package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.fighters.DefaultFighterHandler
import org.bukkit.Material
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

private const val TEAM_NAME = "sniper"
private const val NORMAL_ARROW_MULTIPLIER = 2
private const val ZOOM_ARROW_MULTIPLIER = 5
private const val ARROW_KNOCKBACK_MULTIPLIER = -0.7
private const val ZOOM_KNOCKBACK_MULTIPLIER = -0.2
private const val NORMAL_SHOT_COOLDOWN: Long = 3000
private const val ZOOM_SHOT_COOLDOWN: Long = 7000
class SniperFighterHandler(plugin: Plugin): DefaultFighterHandler {

    private val shotCooldown = HashMap<UUID, Long>()
    private val playerHealCooldown = HashMap<UUID, Long>()
    private val playersOnZoom = mutableListOf<UUID>()

    override fun canHandle(teamName: String) = teamName == TEAM_NAME
    override fun onItemHeldChange(event: PlayerItemHeldEvent) {}
    override fun onPlayerMove(event: PlayerMoveEvent) {}
    override fun onInventoryClick(event: InventoryClickEvent) {}
    override fun onPlayerInteraction(event: PlayerInteractEvent) {
        val player = event.player

        if (event.action.isLeftClick && !hasCooldown(shotCooldown, player.uniqueId)) {
            if (playersOnZoom.contains(player.uniqueId)) {
                shootWithZoom(player)
                zoomOut(player)
            } else {
                shootWithoutZoom(player)
            }

        } else if (event.action.isRightClick && !playersOnZoom.contains(player.uniqueId)) {
           zoomIn(player)
        } else {
          zoomOut(player)
        }
    }

    override fun onProjectileHit(event: ProjectileHitEvent) {}

    private fun hasCooldown(cooldownToVerify : HashMap<UUID,Long>, player: UUID) : Boolean {
        val cooldown = cooldownToVerify[player]
        if(cooldown != null && cooldown < System.currentTimeMillis() ) {
           cooldownToVerify.remove(player)
        }

        return cooldownToVerify[player] != null
    }

    private fun shootWithoutZoom(player: Player) {
        player.launchProjectile(Arrow::class.java, player.location.direction.multiply(NORMAL_ARROW_MULTIPLIER))
        player.velocity = player.location.direction.multiply(ARROW_KNOCKBACK_MULTIPLIER)
        shotCooldown[player.uniqueId] = System.currentTimeMillis() + NORMAL_SHOT_COOLDOWN
        player.setCooldown(Material.IRON_SWORD, (NORMAL_SHOT_COOLDOWN/ 50).toInt())
    }

    private fun shootWithZoom(player: Player) {
        player.launchProjectile(Arrow::class.java, player.location.direction.multiply(ZOOM_ARROW_MULTIPLIER))
        player.velocity = player.location.direction.multiply(ZOOM_KNOCKBACK_MULTIPLIER)
        shotCooldown[player.uniqueId] = System.currentTimeMillis() + ZOOM_SHOT_COOLDOWN
        player.setCooldown(Material.IRON_SWORD, (ZOOM_SHOT_COOLDOWN / 49).toInt())
    }

    private fun zoomIn(player: Player) {
        playersOnZoom.add(player.uniqueId)
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.SLOW,
                PotionEffect.INFINITE_DURATION,
                49,
                true,
                false
            )
        )
    }

    private fun zoomOut(player: Player) {
        playersOnZoom.remove(player.uniqueId)
        player.removePotionEffect(PotionEffectType.SLOW)
    }
}