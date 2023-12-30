package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.fighters.DefaultFighterHandler
import me.nikodemos612.classfight.utill.player.Cooldown
import org.bukkit.Material
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

private const val TEAM_NAME = "sniper"
private const val NORMAL_ARROW_MULTIPLIER = 2
private const val ZOOM_ARROW_MULTIPLIER = 5
private const val ARROW_KNOCKBACK_MULTIPLIER = -0.7
private const val ZOOM_KNOCKBACK_MULTIPLIER = -0.2
private const val NORMAL_SHOT_COOLDOWN = 3000L
private const val ZOOM_SHOT_COOLDOWN = 7000L
private const val HEAL_EFFECT_DURATION = 20 // ticks
private const val HEAL_COOLDOWN = 21000L
class SniperFighterHandler: DefaultFighterHandler {

    private val shotCooldown = Cooldown()
    private val playerHealCooldown = Cooldown()
    private val playersOnZoom = mutableListOf<UUID>()

    override fun canHandle(teamName: String) = teamName == TEAM_NAME

    override fun onItemHeldChange(event: PlayerItemHeldEvent) {
        val player = event.player

        when (event.newSlot) {
            1 -> {
                if (!playerHealCooldown.hasCooldown(player.uniqueId)) {
                    player.useHealAbility()
                }
                player.inventory.heldItemSlot = 0
            }
        }
    }

    override fun onPlayerInteraction(event: PlayerInteractEvent) {
        val player = event.player

        when {
            event.action.isLeftClick -> {
                if (!shotCooldown.hasCooldown(player.uniqueId)) {
                    if (player.hasZoom()) {
                        player.shootWithZoom()
                        player.zoomOut()
                    } else player.shootWithoutZoom()
                }
            }

            event.action.isRightClick -> {
                if (player.hasZoom())
                    player.zoomIn()
                else player.zoomOut()
            }
        }
    }

    override fun onProjectileHit(event: ProjectileHitEvent) {}

    private fun Player.shootWithoutZoom() {
        this.launchProjectile(Arrow::class.java, this.location.direction.multiply(NORMAL_ARROW_MULTIPLIER))
        this.velocity = this.location.direction.multiply(ARROW_KNOCKBACK_MULTIPLIER)

        shotCooldown.addCooldownToPlayer(this.uniqueId, NORMAL_SHOT_COOLDOWN)
        this.setCooldown(Material.IRON_SWORD, (NORMAL_SHOT_COOLDOWN / 50).toInt())
    }

    private fun Player.shootWithZoom() {
        this.launchProjectile(Arrow::class.java, this.location.direction.multiply(ZOOM_ARROW_MULTIPLIER))
        this.velocity = this.location.direction.multiply(ZOOM_KNOCKBACK_MULTIPLIER)

        shotCooldown.addCooldownToPlayer(this.uniqueId, ZOOM_SHOT_COOLDOWN)
        this.setCooldown(Material.IRON_SWORD, (ZOOM_SHOT_COOLDOWN / 50).toInt())
    }

    private fun Player.zoomIn() {
        playersOnZoom.add(this.uniqueId)
        this.addPotionEffect(
                PotionEffect(
                        PotionEffectType.SLOW,
                        PotionEffect.INFINITE_DURATION,
                        49,
                        true,
                        false
                )
        )
    }

    private fun Player.zoomOut() {
        playersOnZoom.remove(this.uniqueId)
        this.removePotionEffect(PotionEffectType.SLOW)
    }


    private fun Player.hasZoom() = playersOnZoom.contains(this.uniqueId)

    private fun Player.useHealAbility() {
        this.addPotionEffect(
                PotionEffect(
                        PotionEffectType.HEAL,
                        HEAL_EFFECT_DURATION,
                        5,
                        true,
                        true,
                        true,
                ),
        )
        playerHealCooldown.addCooldownToPlayer(this.uniqueId, HEAL_COOLDOWN)
    }
}