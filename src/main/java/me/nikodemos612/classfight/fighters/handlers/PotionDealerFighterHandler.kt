package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.utill.BounceProjectileOnHitUseCase
import me.nikodemos612.classfight.utill.player.Cooldown
import me.nikodemos612.classfight.utill.player.MultipleCooldown
import me.nikodemos612.classfight.utill.plugins.safeLet
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.*
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.roundToInt

private const val TEAM_NAME = "potionDealer"

private const val MAX_PRIMARY_POTION = 3
private const val PRIMARY_POTION_VELOCITY_MULTIPLIER = 1.5
private const val DAMAGE_POTION_COOLDOWN = 10000L
private const val DAMAGE_POTION_CLOUD_DURATION = 50
private const val DAMAGE_POTION_CLOUD_AREA = 4.5F
private const val DAMAGE_POTION_CLOUD_RADIOS_PER_TICK = -0.05F
private const val DAMAGE_POTION_MAX_DAMAGE = 12.0
private const val DAMAGE_POTION_MIN_DAMAGE = 4.0
private const val DAMAGE_POTION_WAIT_TIME = 10
private const val DAMAGE_POTION_REAPLICATION_DELAY = 10
private const val BLINDNESS_POTION_COOLDOWN = 9000L
private const val BLINDNESS_POTION_CLOUD_DURATION = 10
private const val BLINDNESS_POTION_CLOUD_RADIOS_PER_TICK = 0.5F
private const val BLINDNESS_POTION_DURATION = 70
private const val BLINDNESS_POTION_AMPLIFIER = 1
private const val BLINDNESS_POTION_WAIT_TIME = 5

private const val SECONDARY_POTION_VELOCITY_MULTIPLIER = 1.0
private const val BOOST_POTION_COOLDOWN = 20000L
private const val BOOST_CLOUD_DURATION = 10
private const val BOOST_SPEED_DURATION = 120
private const val BOOST_SPEED_AMPLIFIER = 0
private const val BOOST_JUMP_DURATION = 120
private const val BOOST_JUMP_AMPLIFIER = 2
private const val BOOST_POTION_WAIT_TIME = 0
private const val HEAL_POTION_COOLDOWN = 30000L
private const val HEAL_CLOUD_DURATION = 10
private const val HEAL_HEAL_AMPLIFIER = 2
private const val HEAL_ABSORPTION_DURATION = 600
private const val HEAL_ABSORPTION_AMPLIFIER = 1
private const val HEAL_POTION_WAIT_TIME = 40

private const val PRIMARY_CLICK_COOLDOWN = 1000L
private const val SECONDARY_CLICK_COOLDOWN = 10L

private const val BOUNCE_FRICTION = 0.25

private const val DAMAGE_POTION_NAME = "Damage Potion"
private const val BLINDNESS_POTION_NAME = "Blindness Potion"
private const val HEALING_POTION_NAME = "Healing Potion"
private const val BOOST_POTION_NAME = "Boost Potion"

/**
 * This class handles the Potion Dealer fighter an all it's events
 * @author Nikodemos0612 (Lucas Coimbra)
 * @see Cooldown
 * @see MultipleCooldown
 * @see BounceProjectileOnHitUseCase
 * @see DefaultFighterHandler
 */
class PotionDealerFighterHandler(private val plugin: Plugin) : DefaultFighterHandler {

    private val primaryPotionCooldown = MultipleCooldown(MAX_PRIMARY_POTION)
    private val primaryClickCooldown = Cooldown()
    private val secondaryPotionCooldown = Cooldown()
    private val secondaryClickCooldown = Cooldown()

    override fun canHandle(teamName: String): Boolean = teamName == TEAM_NAME
    override fun resetInventory(player: Player) {
        player.inventory.clear()
        player.inventory.setItem(0, ItemStack(Material.STICK))
        player.inventory.setItem(1, ItemStack(Material.GLOWSTONE))

        player.inventory.setItem(8, ItemStack(Material.LIME_CONCRETE))
        player.inventory.setItem(7, ItemStack(Material.YELLOW_CONCRETE))
        player.inventory.setItem(6, ItemStack(Material.ORANGE_CONCRETE))
    }

    override fun resetCooldowns(player: Player) {
        val playerUUID = player.uniqueId

        primaryPotionCooldown.resetCooldowns(playerUUID)
        secondaryPotionCooldown.resetCooldown(playerUUID)
        secondaryClickCooldown.resetCooldown(playerUUID)
        player.resetCooldown()
    }

    override fun onItemHeldChange(event: PlayerItemHeldEvent) {}

    override fun onPlayerInteraction(event: PlayerInteractEvent) {
        when (event.player.inventory.heldItemSlot) {
            0 -> handlePrimaryPotionTrow(event)
            1 -> handleSecondaryPotionTrow(event)
        }
    }

    override fun onProjectileHit(event: ProjectileHitEvent) {
        val hitEntity = event.hitEntity
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
        } else if (hitEntity != null) {
            val projectile = event.entity as? Snowball
            projectile?.let {
                when (projectile.customName()) {
                    Component.text(DAMAGE_POTION_NAME) -> activateDamageAreaEffect(hitEntity)
                    Component.text(BLINDNESS_POTION_NAME) -> activateBlindnessAreaEffect(hitEntity)
                    Component.text(HEALING_POTION_NAME) -> activateHealAreaEffect(hitEntity)
                    Component.text(BOOST_POTION_NAME) -> activateBoostAreaEffect(hitEntity)
                }
            }
            event.entity.remove()
        } else {
            BounceProjectileOnHitUseCase(event, BOUNCE_FRICTION)
        }
    }

    override fun onPlayerHitByEntityFromThisTeam(event: EntityDamageByEntityEvent) {
        (event.damager as? AreaEffectCloud)?.let{ potion ->
            when (event.cause) {
                EntityDamageEvent.DamageCause.ENTITY_ATTACK -> {
                    val damageToAddPerTick = (DAMAGE_POTION_MAX_DAMAGE - DAMAGE_POTION_MIN_DAMAGE) /
                            (DAMAGE_POTION_CLOUD_DURATION + DAMAGE_POTION_WAIT_TIME)
                    event.damage = (DAMAGE_POTION_MIN_DAMAGE + (damageToAddPerTick * potion.ticksLived))
                        .roundToInt().toDouble()
                    plugin.logger.info(event.damage.toString())
                }

                else -> {}
            }
        }
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        TODO("Not yet implemented")
    }

    override fun onPlayerDamage(event: EntityDamageEvent) {
        TODO("Not yet implemented")
    }

    /**
     * Handles the throw of the Primary Potion.
     * Verifies what button the player has clicked, then throws the primary potion of that respective button.
     * @param event the event that defines what button the player pressed and the player.
     */
    private fun handlePrimaryPotionTrow(event: PlayerInteractEvent) {
        val player = event.player

        when {
            event.action.isLeftClick -> {
                if (
                    !primaryClickCooldown.hasCooldown(player.uniqueId) &&
                    !primaryPotionCooldown.isAllInCooldown(player.uniqueId)
                ) {
                    primaryClickCooldown.addCooldownToPlayer(player.uniqueId, PRIMARY_CLICK_COOLDOWN)
                    player.setCooldown(
                        player.inventory.getItem(0)?.type ?: Material.BEDROCK,
                        (PRIMARY_CLICK_COOLDOWN / 50).toInt()
                    )
                    shootDamagePotion(player)
                }
            }

            event.action.isRightClick -> {
                if (
                    !secondaryClickCooldown.hasCooldown(player.uniqueId) &&
                    !primaryPotionCooldown.isAllInCooldown(player.uniqueId)
                ) {
                    secondaryClickCooldown.addCooldownToPlayer(player.uniqueId, PRIMARY_CLICK_COOLDOWN)
                    player.setCooldown(
                        player.inventory.getItem(0)?.type ?: Material.BEDROCK,
                        (PRIMARY_CLICK_COOLDOWN / 50).toInt()
                    )
                    shootBlindnessPotion(player)
                }
            }
        }
    }

    /**
     * Handles the throw of the Secondary Potion.
     * Verifies what button the player has clicked, then throws the secondary potion of that respective button.
     * @param event the event that defines what button the player pressed and the player.
     */
    private fun handleSecondaryPotionTrow(event: PlayerInteractEvent) {
        val player = event.player

        when {
            event.action.isLeftClick -> {
                if (
                    !secondaryClickCooldown.hasCooldown(player.uniqueId) &&
                    !secondaryPotionCooldown.hasCooldown(player.uniqueId)
                ) {
                    secondaryClickCooldown.addCooldownToPlayer(player.uniqueId, SECONDARY_CLICK_COOLDOWN)
                    shootHealingPotion(player)
                }
            }

            event.action.isRightClick -> {
                if (
                    !secondaryClickCooldown.hasCooldown(player.uniqueId) &&
                    !secondaryPotionCooldown.hasCooldown(player.uniqueId)
                ) {
                    secondaryClickCooldown.addCooldownToPlayer(player.uniqueId, SECONDARY_CLICK_COOLDOWN)
                    shootBoostPotion(player)
                }
            }
        }
    }

    /**
     * Makes the player shoot a damage potion.
     * @param player the player that will shoot the damage potion.
     */
    private fun shootDamagePotion(player: Player) {
        shootPotion(player, DAMAGE_POTION_NAME, PRIMARY_POTION_VELOCITY_MULTIPLIER)
        addToPrimaryCooldown(player, DAMAGE_POTION_COOLDOWN)
    }

    /**
     * Makes the player shoot a blindness potion.
     * @param player the player that will shoot the blindness potion.
     */
    private fun shootBlindnessPotion(player: Player) {
        shootPotion(player, BLINDNESS_POTION_NAME, PRIMARY_POTION_VELOCITY_MULTIPLIER)
        addToPrimaryCooldown(player, BLINDNESS_POTION_COOLDOWN)
    }

    /**
     * Makes the player shoot a boost potion.
     * @param player the player that will shoot the boost potion.
     */
    private fun shootBoostPotion(player: Player) {
       shootPotion(player, BOOST_POTION_NAME, SECONDARY_POTION_VELOCITY_MULTIPLIER)
       addToSecondaryCooldown(player, BOOST_POTION_COOLDOWN)
    }


    /**
     * Makes the player shoot a healing potion.
     * @param player the player that will shoot the healing potion.
     */
    private fun shootHealingPotion(player: Player) {
        shootPotion(player, HEALING_POTION_NAME, SECONDARY_POTION_VELOCITY_MULTIPLIER)
        addToSecondaryCooldown(player, HEAL_POTION_COOLDOWN)
    }

    /**
     * Adds a cooldown to the primary potion, given a player.
     * @param player the player that will receive the cooldown.
     * @param cooldown the cooldown in milliseconds.
     */
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

    /**
     * Adds a cooldown to the secondary potion, given a player
     * @param player the player that will receive the cooldown.
     * @param cooldown the cooldown in milliseconds.
     */
    private fun addToSecondaryCooldown(player: Player, cooldown: Long) {
        secondaryPotionCooldown.addCooldownToPlayer(player.uniqueId, cooldown)
        player.setCooldown(
                player.inventory.getItem(1)?.type ?: Material.BEDROCK,
                (cooldown/ 50).toInt()
        )
    }

    /**
     * Makes the player shoot a potion given the desired potion name and throw velocity.
     * @param player the player that will throw the potion.
     * @param potionName the name of the effect that the potion gives.
     * @param potionVelocityMultiplier the multiplier of the velocity of the potion on throw.
     */
    private fun shootPotion(player: Player, potionName: String, potionVelocityMultiplier: Double) = player.launchProjectile(
        Snowball::class.java,
        player.location.direction.multiply(potionVelocityMultiplier)
    ).customName(Component.text(potionName))

    /**
     * Activates the damage potion area effect.
     * @param potion the potion that will spawn a damage cloud.
     */
    private fun activateDamageAreaEffect(potion: Entity) {
        potion.world.spawn(potion.location, AreaEffectCloud::class.java).let { cloud ->
            cloud.radiusPerTick = DAMAGE_POTION_CLOUD_RADIOS_PER_TICK
            cloud.duration = DAMAGE_POTION_CLOUD_DURATION
            cloud.addCustomEffect(PotionEffect(PotionEffectType.HARM, 0, 1), false)
            ((potion as? ThrowableProjectile)?.shooter as? Player)?.let { cloud.ownerUniqueId = it.uniqueId }
            cloud.radius = DAMAGE_POTION_CLOUD_AREA
            cloud.waitTime = DAMAGE_POTION_WAIT_TIME
            cloud.reapplicationDelay = DAMAGE_POTION_REAPLICATION_DELAY
        }
    }

    /**
     * Activates the blindness potion area effect.
     * @param potion the potion that will spawn a blindness cloud.
     */
    private fun activateBlindnessAreaEffect(potion: Entity) {
        potion.world.spawn(potion.location, AreaEffectCloud::class.java).let { cloud ->
            cloud.radiusPerTick = BLINDNESS_POTION_CLOUD_RADIOS_PER_TICK
            cloud.duration = BLINDNESS_POTION_CLOUD_DURATION
            cloud.addCustomEffect(
                PotionEffect(PotionEffectType.BLINDNESS, BLINDNESS_POTION_DURATION, BLINDNESS_POTION_AMPLIFIER),
                true
            )
            ((potion as? ThrowableProjectile)?.shooter as? Player)?.let { cloud.ownerUniqueId = it.uniqueId }
            cloud.waitTime = BLINDNESS_POTION_WAIT_TIME
        }
    }

    /**
     * Activates the boost area effect.
     * @param potion the potion that will spawn a boost cloud.
     */
    private fun activateBoostAreaEffect(potion: Entity) {
        potion.world.spawn(potion.location, AreaEffectCloud::class.java).let { cloud ->
            cloud.duration = BOOST_CLOUD_DURATION
            cloud.addCustomEffect(PotionEffect(PotionEffectType.SPEED, BOOST_SPEED_DURATION, BOOST_SPEED_AMPLIFIER), true)
            cloud.addCustomEffect(PotionEffect(PotionEffectType.JUMP, BOOST_JUMP_DURATION, BOOST_JUMP_AMPLIFIER), true)
            ((potion as? ThrowableProjectile)?.shooter as? Player)?.let { cloud.ownerUniqueId = it.uniqueId }
            cloud.waitTime = BOOST_POTION_WAIT_TIME
        }
    }

    /**
     * Activates the heal area effect.
     * @param potion the potion that will spawn a heal cloud.
     */
    private fun activateHealAreaEffect(potion: Entity) {
        potion.world.spawn(potion.location, AreaEffectCloud::class.java).let { cloud ->
            cloud.duration = HEAL_CLOUD_DURATION
            cloud.addCustomEffect(PotionEffect(PotionEffectType.HEAL, 1, HEAL_HEAL_AMPLIFIER), false)
            cloud.addCustomEffect(
                PotionEffect(PotionEffectType.ABSORPTION, HEAL_ABSORPTION_DURATION, HEAL_ABSORPTION_AMPLIFIER),
                true
            )
            ((potion as? ThrowableProjectile)?.shooter as? Player)?.let { cloud.ownerUniqueId = it.uniqueId }
            cloud.waitTime = HEAL_POTION_WAIT_TIME
        }
    }

}