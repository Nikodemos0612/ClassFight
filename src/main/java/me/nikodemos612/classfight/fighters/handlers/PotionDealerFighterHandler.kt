package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.fighters.DefaultFighterHandler
import me.nikodemos612.classfight.utill.BounceProjectileOnHitUseCase
import org.bukkit.Color
import org.bukkit.block.BlockFace
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.ThrownPotion
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

private const val TEAM_NAME = "potionDealer"

class PotionDealerFighterHandler() : DefaultFighterHandler {
    override fun canHandle(teamName: String): Boolean = teamName == TEAM_NAME

    override fun onItemHeldChange(event: PlayerItemHeldEvent) {
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
    }

    override fun onInventoryClick(event: InventoryClickEvent) {
    }

    override fun onPlayerInteraction(event: PlayerInteractEvent) {
        val player = event.player
        if (event.action.isLeftClick) {
            if (player.inventory.heldItemSlot == 0)
                player.launchProjectile(ThrownPotion::class.java, player.location.direction).potionMeta.color =
                    Color.RED
            else
                player.launchProjectile(ThrownPotion::class.java, player.location.direction).potionMeta.color =
                    Color.GREEN
        } else if (event.action.isRightClick) {
            if (player.inventory.heldItemSlot == 0)
                player.launchProjectile(ThrownPotion::class.java, player.location.direction).potionMeta.color =
                    Color.BLACK
            else
                player.launchProjectile(ThrownPotion::class.java, player.location.direction).potionMeta.color =
                    Color.WHITE
        }
    }

    override fun onProjectileHit(event: ProjectileHitEvent) {
        if (event.hitBlockFace == BlockFace.UP) {
            val projectile = event.entity as? ThrownPotion
            projectile?.let {
                when (projectile.potionMeta.color) {
                    Color.RED -> activateDamageAreaEffect(projectile)
                    Color.BLACK -> activateBlindnessEffect(projectile)
                    Color.GREEN -> activateBoostEffect(projectile)
                    Color.WHITE -> activateHealEffect(projectile)
                }
               projectile.splash()
            } ?: event.entity.remove()
        } else {
            BounceProjectileOnHitUseCase(event)
        }
    }

    private fun activateDamageAreaEffect(potion: ThrownPotion) {
        val cloud = potion.world.spawn(potion.location, AreaEffectCloud::class.java)
        cloud.duration = 100
        cloud.addCustomEffect(PotionEffect(PotionEffectType.HARM, 0, 2), false)
    }

    private fun activateBlindnessEffect(potion: ThrownPotion) {
        val cloud = potion.world.spawn(potion.location, AreaEffectCloud::class.java)
        cloud.duration = 50
        cloud.addCustomEffect(PotionEffect(PotionEffectType.BLINDNESS, 0, 1), true)
    }

    private fun activateBoostEffect(potion: ThrownPotion) {
        val cloud = potion.world.spawn(potion.location, AreaEffectCloud::class.java)
        cloud.duration = 25
        cloud.addCustomEffect(PotionEffect(PotionEffectType.SPEED, 50, 2), true)
        cloud.addCustomEffect(PotionEffect(PotionEffectType.JUMP, 50, 2), true)
    }

    private fun activateHealEffect(potion: ThrownPotion) {
        val cloud = potion.world.spawn(potion.location, AreaEffectCloud::class.java)
        cloud.duration = 25
        cloud.addCustomEffect(PotionEffect(PotionEffectType.HEAL, 0, 3), false)
        cloud.addCustomEffect(PotionEffect(PotionEffectType.ABSORPTION, 100, 1), true)
    }

}