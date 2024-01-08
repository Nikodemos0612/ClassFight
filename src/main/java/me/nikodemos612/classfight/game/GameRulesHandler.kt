package me.nikodemos612.classfight.game

import io.papermc.paper.event.player.PlayerPickItemEvent
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

private object SpawnLocation {
    const val X = 112.0
    const val Y = 43.0
    const val Z = 31.0
}
private const val INVULNERABILITY_DURATION = 1000

/**
 * This class handles all the rules necessary to make the game run properly
 * @author Nikodemos0612 (Lucas Coimbra)
 */
class GameRulesHandler: Listener {
    @EventHandler
    fun onPlayerJoin(event : PlayerJoinEvent) {
        val player = event.player
        player.teleport(Location(player.world, SpawnLocation.X, SpawnLocation.Y, SpawnLocation.Z))
        player.saturation = 0f
    }

    @EventHandler
    fun onPlayerInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked
        if (player is Player && event.clickedInventory != null && player.gameMode != GameMode.CREATIVE)
            event.isCancelled = true
    }

    @EventHandler
    fun onPlayerItemPickup(event: PlayerPickItemEvent) {
        if (event.player.gameMode != GameMode.CREATIVE)
            event.isCancelled = true
    }

    @EventHandler
    fun onPlayerItemDrop(event: PlayerDropItemEvent) {
        if (event.player.gameMode != GameMode.CREATIVE)
            event.isCancelled = true
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        player.saturation = 0f
        player.clearActivePotionEffects()
        player.addPotionEffect(PotionEffect(
            PotionEffectType.DAMAGE_RESISTANCE,
            INVULNERABILITY_DURATION,
            Int.MAX_VALUE
        ))
    }

    @EventHandler
    fun onPlayerHungerLevelChange(event: FoodLevelChangeEvent) {
        if (event.entity is Player)
            event.isCancelled = true
    }
}