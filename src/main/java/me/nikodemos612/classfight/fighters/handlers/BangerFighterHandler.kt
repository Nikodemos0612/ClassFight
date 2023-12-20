package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.ClassFight
import me.nikodemos612.classfight.fighters.DefaultFighterHandler
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.Plugin

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

    private fun explodeFlashBangProjectileFromPlayer(player: Player) = Runnable {
        for (entity in player.world.entities) {
            if (entity is Snowball) {
                val shooter = entity.shooter

                if (shooter is Player && shooter.uniqueId == player.uniqueId) {
                    entity.remove()
                }
            }
        }
    }
}