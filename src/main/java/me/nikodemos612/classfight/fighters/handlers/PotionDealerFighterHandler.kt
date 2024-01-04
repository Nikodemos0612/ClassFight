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
private const val PRIMARY_POTION_VELOCITY_MULTIPLIER = 1.5
private const val DAMAGE_POTION_COOLDOWN = 7000L
private const val DAMAGE_POTION_CLOUD_DURATION = 100
private const val DAMAGE_POTION_AMPLIFIER = 2
private const val BLINDNESS_POTION_COOLDOWN = 3000L
private const val BLINDNESS_POTION_CLOUD_DURATION = 50
private const val BLINDNESS_POTION_CLOUD_RADIOS_PER_TICK = 0.05F
private const val BLINDNESS_POTION_DURATION = 50
private const val BLINDNESS_POTION_AMPLIFIER = 1

private const val SECONDARY_POTION_VELOCITY_MULTIPLIER = 1.0
private const val BOOST_POTION_COOLDOWN = 20000L
private const val BOOST_CLOUD_DURATION = 25
private const val BOOST_SPEED_DURATION = 50
private const val BOOST_SPEED_AMPLIFIER = 2
private const val BOOST_JUMP_DURATION = 50
private const val BOOST_JUMP_AMPLIFIER = 2
private const val HEAL_POTION_COOLDOWN = 30000L
private const val HEAL_CLOUD_DURATION = 25
private const val HEAL_HEAL_AMPLIFIER = 3
private const val HEAL_ABSORPTION_DURATION = 100
private const val HEAL_ABSORPTION_AMPLIFIER = 1


private const val RIGHT_CLICK_COOLDOWN = 10L

private const val DAMAGE_POTION_NAME = "Damage Potion"
private const val BLINDNESS_POTION_NAME = "Blindness Potion"
private const val HEALING_POTION_NAME = "Healing Potion"
private const val BOOST_POTION_NAME = "Boost Potion"

class PotionDealerFighterHandler : DefaultFighterHandler {

    private val primaryPotionCooldown = MultipleCooldown(MAX_PRIMARY_POTION)
    private val secondaryPotionCooldown = Cooldown()
    private val rightClickCooldown = Cooldown()

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
                when (projectile.customName()) {
                    Component.text(DAMAGE_POTION_NAME) -> activateDamageAreaEffect(projectile)
                    Component.text(BLINDNESS_POTION_NAME) -> activateBlindnessAreaEffect(projectile)
                    Component.text(HEALING_POTION_NAME) -> activateHealAreaEffect(projectile)
                    Component.text(BOOST_POTION_NAME) -> activateBoostAreaEffect(projectile)
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
                    shootDamagePotion(player)

            }

            event.action.isRightClick -> {
                if (
                    !rightClickCooldown.hasCooldown(player.uniqueId) &&
                    !primaryPotionCooldown.isAllInCooldown(player.uniqueId)
                ) {
                    rightClickCooldown.addCooldownToPlayer(player.uniqueId, RIGHT_CLICK_COOLDOWN)
                    shootBlindnessPotion(player)
                }
            }
        }
    }

    private fun handleSecondaryPotionTrow(event: PlayerInteractEvent) {
        val player = event.player

        when {
            event.action.isLeftClick -> {
                if (!secondaryPotionCooldown.hasCooldown(player.uniqueId))
                    shootHealingPotion(player)
            }

            event.action.isRightClick -> {
                if (
                    !rightClickCooldown.hasCooldown(player.uniqueId) &&
                    !secondaryPotionCooldown.hasCooldown(player.uniqueId)
                ) {
                    rightClickCooldown.addCooldownToPlayer(player.uniqueId, RIGHT_CLICK_COOLDOWN)
                    shootBoostPotion(player)
                }
            }
        }
    }

    private fun shootDamagePotion(player: Player) {
        shootPotion(player, DAMAGE_POTION_NAME, PRIMARY_POTION_VELOCITY_MULTIPLIER)
        addToPrimaryCooldown(player, DAMAGE_POTION_COOLDOWN)
    }

    private fun shootBlindnessPotion(player: Player) {
        shootPotion(player, BLINDNESS_POTION_NAME, PRIMARY_POTION_VELOCITY_MULTIPLIER)
        addToPrimaryCooldown(player, BLINDNESS_POTION_COOLDOWN)
    }

    private fun shootBoostPotion(player: Player) {
       shootPotion(player, BOOST_POTION_NAME, SECONDARY_POTION_VELOCITY_MULTIPLIER)
       addToSecondaryCooldown(player, BOOST_POTION_COOLDOWN)
    }

    private fun shootHealingPotion(player: Player) {
        shootPotion(player, HEALING_POTION_NAME, SECONDARY_POTION_VELOCITY_MULTIPLIER)
        addToSecondaryCooldown(player, HEAL_POTION_COOLDOWN)
    }

    private fun addToPrimaryCooldown(player: Player, cooldown: Long) {
        primaryPotionCooldown.addCooldown(player.uniqueId, cooldown)

        for (inventoryIndex in 8.downTo(9 - MAX_PRIMARY_POTION)) {
            player.inventory.getItem(inventoryIndex)?.type?.let {
                if (!player.hasCooldown(it)) {
                    player.setCooldown(it, (cooldown/ 50).toInt())
                    return
                }
            }
        }
    }

    private fun addToSecondaryCooldown(player: Player, cooldown: Long) {
        secondaryPotionCooldown.addCooldownToPlayer(player.uniqueId, cooldown)
        player.setCooldown(
                player.inventory.getItem(1)?.type ?: Material.BEDROCK,
                (cooldown/ 50).toInt()
        )
    }

    private fun shootPotion(player: Player, potionName: String, potionVelocityMultiplier: Double) = player.launchProjectile(
        Snowball::class.java,
        player.location.direction.multiply(potionVelocityMultiplier)
    ).customName(Component.text(potionName))

    private fun activateDamageAreaEffect(potion: Entity) {
        val cloud = potion.world.spawn(potion.location, AreaEffectCloud::class.java)
        cloud.duration = DAMAGE_POTION_CLOUD_DURATION
        cloud.addCustomEffect(PotionEffect(PotionEffectType.HARM, 0, DAMAGE_POTION_AMPLIFIER ), false)
    }

    private fun activateBlindnessAreaEffect(potion: Entity) {
        val cloud = potion.world.spawn(potion.location, AreaEffectCloud::class.java)
        cloud.radiusPerTick = BLINDNESS_POTION_CLOUD_RADIOS_PER_TICK
        cloud.duration = BLINDNESS_POTION_CLOUD_DURATION
        cloud.addCustomEffect(
            PotionEffect(PotionEffectType.BLINDNESS, BLINDNESS_POTION_DURATION, BLINDNESS_POTION_AMPLIFIER),
            true
        )
    }

    private fun activateBoostAreaEffect(potion: Entity) {
        val cloud = potion.world.spawn(potion.location, AreaEffectCloud::class.java)
        cloud.duration = BOOST_CLOUD_DURATION
        cloud.addCustomEffect(PotionEffect(PotionEffectType.SPEED, BOOST_SPEED_DURATION, BOOST_SPEED_AMPLIFIER), true)
        cloud.addCustomEffect(PotionEffect(PotionEffectType.JUMP, BOOST_JUMP_DURATION, BOOST_JUMP_AMPLIFIER), true)
    }

    private fun activateHealAreaEffect(potion: Entity) {
        val cloud = potion.world.spawn(potion.location, AreaEffectCloud::class.java)
        cloud.duration = HEAL_CLOUD_DURATION
        cloud.addCustomEffect(PotionEffect(PotionEffectType.HEAL, 1, HEAL_HEAL_AMPLIFIER), false)
        cloud.addCustomEffect(
            PotionEffect(PotionEffectType.ABSORPTION, HEAL_ABSORPTION_DURATION, HEAL_ABSORPTION_AMPLIFIER),
            true
        )
    }

}