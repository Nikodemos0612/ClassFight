package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.fighters.DefaultFighterHandler
import me.nikodemos612.classfight.utill.BounceProjectileOnHitUseCase
import me.nikodemos612.classfight.utill.player.Cooldown
import me.nikodemos612.classfight.utill.player.MultipleCooldown
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.*
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

private const val TEAM_NAME = "potionDealer"

private const val MAX_PRIMARY_POTION = 3
private const val PRIMARY_POTION_VELOCITY_MULTIPLIER = 2.0
private const val PRIMARY_POTION_COOLDOWN = 7000L
private const val SECONDARY_POTION_VELOCITY_MULTIPLIER = 1.5
private const val SECONDARY_POTION_COOLDOWN = 21000L

private const val DAMAGE_POTION_NAME = "Damage Potion"
private const val BLINDNESS_POTION_NAME = "Blindness Potion"
private const val HEALING_POTION_NAME = "Healing Potion"
private const val BOOST_POTION_NAME = "Boost Potion"

class PotionDealerFighterHandler : DefaultFighterHandler {

    private val primaryPotionCooldown = MultipleCooldown(MAX_PRIMARY_POTION)
    private val secondaryPotionCooldown = Cooldown()

    override fun canHandle(teamName: String): Boolean = teamName == TEAM_NAME

    override fun onItemHeldChange(event: PlayerItemHeldEvent) {
    }

    override fun onPlayerInteraction(event: PlayerInteractEvent) {
        when (event.player.inventory.heldItemSlot) {
            0 -> handlePrimaryPotionTrow(event)
            1 -> handleSecondaryPotionTrow(event)
        }
    }

    override fun onProjectileHit(event: ProjectileHitEvent) {
        if (event.hitBlockFace == BlockFace.UP) {
            val projectile = event.entity as? Snowball
            projectile?.let {
                when (projectile.item.itemMeta.displayName()) {
                    Component.text(DAMAGE_POTION_NAME) -> projectile.activateDamageAreaEffect()
                    Component.text(BLINDNESS_POTION_NAME) -> projectile.activateBlindnessAreaEffect()
                    Component.text(HEALING_POTION_NAME) -> projectile.activateHealAreaEffect()
                    Component.text(BOOST_POTION_NAME) -> projectile.activateBoostAreaEffect()
                }
            }
            event.entity.remove()
        } else {
            BounceProjectileOnHitUseCase(event)
        }
    }

    private fun handlePrimaryPotionTrow(event: PlayerInteractEvent) {
        val player = event.player

        when {
            event.action.isLeftClick -> {
                if (!primaryPotionCooldown.isAllInCooldown(player.uniqueId))
                    player.shootDamagePotion()

            }

            event.action.isRightClick -> {
                if (!primaryPotionCooldown.isAllInCooldown(player.uniqueId))
                    player.shootBlindnessPotion()
            }
        }
    }

    private fun handleSecondaryPotionTrow(event: PlayerInteractEvent) {
        val player = event.player

        when {
            event.action.isLeftClick -> {
                if (!secondaryPotionCooldown.hasCooldown(player.uniqueId))
                    player.shootHealingPotion()
            }

            event.action.isRightClick -> {
                if (!secondaryPotionCooldown.hasCooldown(player.uniqueId))
                    player.shootBoostPotion()
            }
        }
    }

    private fun Player.shootDamagePotion() {
        this.shootPotion(DAMAGE_POTION_NAME, PRIMARY_POTION_VELOCITY_MULTIPLIER)
        this.addToPrimaryCooldown()
    }

    private fun Player.shootBlindnessPotion() {
        this.shootPotion(BLINDNESS_POTION_NAME, PRIMARY_POTION_VELOCITY_MULTIPLIER)
        this.addToPrimaryCooldown()
    }

    private fun Player.shootBoostPotion() {
       this.shootPotion(BOOST_POTION_NAME, SECONDARY_POTION_VELOCITY_MULTIPLIER)
       this.addToSecondaryCooldown()
    }

    private fun Player.shootHealingPotion() {
        this.shootPotion(HEALING_POTION_NAME, SECONDARY_POTION_VELOCITY_MULTIPLIER)
        this.addToSecondaryCooldown()
    }

    private fun Player.addToPrimaryCooldown() {
        primaryPotionCooldown.addCooldown(this.uniqueId, PRIMARY_POTION_COOLDOWN)
        val cooldownInTicks = (PRIMARY_POTION_COOLDOWN / 50).toInt()
        primaryPotionCooldown.getCooldownQuantity(this.uniqueId).let {
            this.setCooldown(
                    this.inventory.getItem(8 - it)?.type ?: Material.BEDROCK,
                    cooldownInTicks
            )
        }
    }

    private fun Player.addToSecondaryCooldown() {
        secondaryPotionCooldown.addCooldownToPlayer(this.uniqueId, SECONDARY_POTION_COOLDOWN)
        this.setCooldown(
                this.inventory.getItem(1)?.type ?: Material.BEDROCK,
                (SECONDARY_POTION_COOLDOWN / 50).toInt()
        )
    }

    private fun Player.shootPotion(potionName: String, potionVelocityMultiplier: Double) = this.launchProjectile(
            Snowball::class.java,
            this.location.direction.multiply(potionVelocityMultiplier)
    ).let {
        val potionMeta = it.item.itemMeta
        potionMeta.displayName(Component.text(potionName))
        it.item.itemMeta = potionMeta
    }

    private fun Entity.activateDamageAreaEffect() {
        val cloud = this.world.spawn(this.location, AreaEffectCloud::class.java)
        cloud.duration = 100
        cloud.addCustomEffect(PotionEffect(PotionEffectType.HARM, 0, 2), false)
    }

    private fun Entity.activateBlindnessAreaEffect() {
        val cloud = this.world.spawn(this.location, AreaEffectCloud::class.java)
        cloud.duration = 50
        cloud.addCustomEffect(PotionEffect(PotionEffectType.BLINDNESS, 0, 1), true)
    }

    private fun Entity.activateBoostAreaEffect() {
        val cloud = this.world.spawn(this.location, AreaEffectCloud::class.java)
        cloud.duration = 25
        cloud.addCustomEffect(PotionEffect(PotionEffectType.SPEED, 50, 2), true)
        cloud.addCustomEffect(PotionEffect(PotionEffectType.JUMP, 50, 2), true)
    }

    private fun Entity.activateHealAreaEffect() {
        val cloud = this.world.spawn(this.location, AreaEffectCloud::class.java)
        cloud.duration = 25
        cloud.addCustomEffect(PotionEffect(PotionEffectType.HEAL, 0, 3), false)
        cloud.addCustomEffect(PotionEffect(PotionEffectType.ABSORPTION, 100, 1), true)
    }

}