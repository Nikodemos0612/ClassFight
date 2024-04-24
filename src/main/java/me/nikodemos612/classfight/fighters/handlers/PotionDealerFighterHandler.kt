package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.fighters.DefaultFighterHandler
import me.nikodemos612.classfight.utill.BounceProjectileOnHitUseCase
import me.nikodemos612.classfight.utill.HealPlayerUseCase
import me.nikodemos612.classfight.utill.cooldown.Cooldown
import me.nikodemos612.classfight.utill.cooldown.MultipleCooldown
import me.nikodemos612.classfight.utill.plugins.iterateRunLater
import me.nikodemos612.classfight.utill.plugins.runAsync
import me.nikodemos612.classfight.utill.plugins.runAsyncLater
import me.nikodemos612.classfight.utill.plugins.runLater
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.Particle.DustTransition
import org.bukkit.block.BlockFace
import org.bukkit.entity.*
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

private const val MAX_PRIMARY_POTION = 3
private const val PRIMARY_POTION_VELOCITY_MULTIPLIER = 1.5
private const val DAMAGE_POTION_COOLDOWN = 10000L
private const val DAMAGE_POTION_CLOUD_AREA = 5.0
private const val DAMAGE_POTION_CLOUD_HEIGHT_RADIUS = 3.0
private const val DAMAGE_POTION_FIRE_PARTICLE_QUANTITY = 400
private const val DAMAGE_POTION_DAMAGE = 5.0
private const val DAMAGE_POTION_DAMAGE_WHEN_BLINDED = 8.0
private const val DAMAGE_POTION_WAIT_TIME = 5L
private const val DAMAGE_POTION_DAMAGE_TICKS = 3
private const val DAMAGE_POTION_DAMAGE_TICKS_DELAY = 20L
private const val BLINDNESS_POTION_COOLDOWN = 11000L
private const val BLINDNESS_POTION_PARTICLE_QUANTITY = 400
private const val BLINDNESS_POTION_DURATION = 100
private const val BLINDNESS_POTION_AMPLIFIER = 1
private const val BLINDNESS_POTION_WAIT_TIME = 5
private const val BLINDNESS_POTION_AREA = 5.5

private const val SECONDARY_POTION_VELOCITY_MULTIPLIER = 1.0
private const val HEAL_POTION_COOLDOWN = 20000L
private const val HEAL_POTION_CLOUD_AREA = 4.5
private const val HEAL_POTION_PARTICLE_QUANTITY = 400
private const val HEAL_POTION_HEAL_AMOUNT_PER_TICK = 4.0
private const val KNOCKBAK_HEAL_MULTIPLIER = 1.5
private const val HEAL_POTION_TICKS = 3
private const val HEAL_POTION_WAIT_TIME = 5L
private const val HEAL_POTION_TICKS_DELAY = 15L

private const val PRIMARY_CLICK_COOLDOWN = 3000L
private const val SECONDARY_CLICK_COOLDOWN = 1000L

private const val BOUNCE_FRICTION = 0.25

private const val DAMAGE_POTION_NAME = "Damage Potion"
private const val BLINDNESS_POTION_NAME = "Blindness Potion"
private const val HEALING_POTION_NAME = "Healing Potion"

private const val POTION_EXPLOSION_PARTICLE_QUANTITY = 200

typealias PlayerUUID = UUID
typealias PotionUUID = UUID

/**
 * This class handles the Potion Dealer fighter an all it's events
 * @author Nikodemos0612 (Lucas Coimbra)
 * @see Cooldown
 * @see MultipleCooldown
 * @see BounceProjectileOnHitUseCase
 * @see DefaultFighterHandler
 */
class PotionDealerFighterHandler(private val plugin: Plugin) : DefaultFighterHandler() {

    private val primaryPotionCooldown = MultipleCooldown(MAX_PRIMARY_POTION)
    private val clickCooldown = Cooldown()
    private val peopleBlindedByPotions = HashMap<PlayerUUID, MutableList<PotionUUID>>()

    private val secondaryPotionCooldown = Cooldown()

    override val fighterTeamName = "potionDealer"

    override fun resetInventory(player: Player) {
        player.inventory.clear()
        player.inventory.setItem(0, ItemStack(Material.STICK))
        player.inventory.setItem(1, ItemStack(Material.GLOWSTONE))

        player.inventory.setItem(8, ItemStack(Material.LIME_CONCRETE))
        player.inventory.setItem(7, ItemStack(Material.YELLOW_CONCRETE))
        player.inventory.setItem(6, ItemStack(Material.ORANGE_CONCRETE))

        player.flySpeed = 0.1F
    }

    override fun resetCooldowns(player: Player) {
        val playerUUID = player.uniqueId

        primaryPotionCooldown.resetCooldowns(playerUUID)
        secondaryPotionCooldown.resetCooldown(playerUUID)
        player.resetCooldown()
    }

    override fun onItemHeldChange(event: PlayerItemHeldEvent) {
        when (event.newSlot) {
            1 -> {
                handleHealingPotionThrow(event.player)
            }
        }
        event.player.inventory.heldItemSlot = 0
    }

    override fun onPlayerInteraction(event: PlayerInteractEvent) {
        val player = event.player
        when {
            event.action.isLeftClick -> handleLeftClick(player)
            event.action.isRightClick -> handleRightClick(player)
        }
    }

    override fun onProjectileHit(event: ProjectileHitEvent) {
        val hitEntity = event.hitEntity
        val projectile = event.entity as? Snowball
        if (hitEntity != null) {
            projectile?.let {
                when (it.customName()) {
                    Component.text(DAMAGE_POTION_NAME) -> activateDamageAreaEffect(projectile, hitEntity.location)
                    Component.text(BLINDNESS_POTION_NAME) -> activateBlindnessAreaEffect(projectile, hitEntity.location)
                    Component.text(HEALING_POTION_NAME) -> activateHealAreaEffect(projectile ,hitEntity.location)
                }
            }
            event.entity.remove()
        } else if (event.hitBlockFace == BlockFace.UP) {
            projectile?.let {
                when (projectile.customName()) {
                    Component.text(DAMAGE_POTION_NAME) -> activateDamageAreaEffect(it, it.location)
                    Component.text(BLINDNESS_POTION_NAME) -> activateBlindnessAreaEffect(it, it.location)
                    Component.text(HEALING_POTION_NAME) -> activateHealAreaEffect(it, it.location)
                }
            }
            event.entity.remove()
        } else {
            BounceProjectileOnHitUseCase(event, BOUNCE_FRICTION)
            projectile?.ownerUniqueId?.let { ownerUUID->
                Bukkit.getPlayer(ownerUUID)?.let { owner ->
                    owner.playSound(owner, Sound.ENTITY_SLIME_SQUISH, 10f, 1f)
                }
            }
        }
    }

    override fun onPlayerHitByEntityFromThisTeam(event: EntityDamageByEntityEvent) {
        when (event.cause) {
            EntityDamageEvent.DamageCause.ENTITY_ATTACK -> (event.damager as? Player)?.let {
                handleLeftClick(player = it)
                event.damage = 0.0
            }

            else -> {}
        }
    }
    private fun handleLeftClick(player: Player) {
        if (
            !clickCooldown.hasCooldown(player.uniqueId) &&
            !primaryPotionCooldown.isAllInCooldown(player.uniqueId)
        ) {
            clickCooldown.addCooldownToPlayer(player.uniqueId, PRIMARY_CLICK_COOLDOWN)
            player.setCooldown(
                player.inventory.getItem(0)?.type ?: Material.BEDROCK,
                (PRIMARY_CLICK_COOLDOWN/ 50).toInt()
            )
            shootDamagePotion(player)
        }
    }
    private fun handleRightClick(player: Player) {
        if (
            !clickCooldown.hasCooldown(player.uniqueId) &&
            !primaryPotionCooldown.isAllInCooldown(player.uniqueId)
        ) {
            clickCooldown.addCooldownToPlayer(player.uniqueId, SECONDARY_CLICK_COOLDOWN)
            player.setCooldown(
                player.inventory.getItem(0)?.type ?: Material.BEDROCK,
                (SECONDARY_CLICK_COOLDOWN / 50).toInt()
            )
            shootBlindnessPotion(player)
        }
    }

    private fun handleHealingPotionThrow(player: Player) {
        if (
            !secondaryPotionCooldown.hasCooldown(player.uniqueId)
        ) {
            shootHealingPotion(player)
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
     * Makes the player shoot a healing potion.
     * @param player the player that will shoot the healing potion.
     */
    private fun shootHealingPotion(player: Player) {
        shootPotion(player, HEALING_POTION_NAME, SECONDARY_POTION_VELOCITY_MULTIPLIER)
        addToSecondaryCooldown(player)
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
     */
    private fun addToSecondaryCooldown(player: Player) {
        secondaryPotionCooldown.addCooldownToPlayer(player.uniqueId, HEAL_POTION_COOLDOWN)
        player.setCooldown(
                player.inventory.getItem(1)?.type ?: Material.BEDROCK,
                (HEAL_POTION_COOLDOWN/ 50).toInt()
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
    private fun activateDamageAreaEffect(
        potion: Entity,
        location: Location
    ) {
        potion.world.spawn(location, AreaEffectCloud::class.java).let { cloud ->
            cloud.duration = (DAMAGE_POTION_DAMAGE_TICKS * DAMAGE_POTION_DAMAGE_TICKS_DELAY + DAMAGE_POTION_WAIT_TIME)
                .toInt()
            cloud.waitTime = 0
            cloud.setParticle(Particle.DUST_COLOR_TRANSITION, DustTransition(Color.ORANGE, Color.PURPLE, 1f))
            cloud.radius = DAMAGE_POTION_CLOUD_AREA.toFloat()
            ((potion as? ThrowableProjectile)?.shooter as? Player)?.let { player ->
                cloud.ownerUniqueId = player.uniqueId
                player.playSound(player, Sound.ENTITY_SPLASH_POTION_BREAK, 10f, 1f)
                createDamagePotionEffectTask(cloud, player)
            }
        }
    }

    private fun createDamagePotionEffectTask(
        potion: AreaEffectCloud,
        potionOwner: Player,
    ) = iterateRunLater(
        plugin = plugin,
        initialTicksDelay = DAMAGE_POTION_WAIT_TIME,
        iterationsTickDelay = DAMAGE_POTION_DAMAGE_TICKS_DELAY,
        iterations = DAMAGE_POTION_DAMAGE_TICKS,
    ) {
        val hitEntities = DAMAGE_POTION_CLOUD_AREA.let {
            potion.location.getNearbyEntities(it, DAMAGE_POTION_CLOUD_HEIGHT_RADIUS, it)
        }

        val potionLineOfSightLocation = potion.location.let {
            it.y += 0.5
            it
        }

        for (entity in hitEntities) {
            if (
                entity is Player &&
                entity.uniqueId != potion.ownerUniqueId &&
                entity.hasLineOfSight(potionLineOfSightLocation)
            ) {
                if (
                    entity.hasPotionEffect(PotionEffectType.BLINDNESS) &&
                    peopleBlindedByPotions[entity.uniqueId] != null
                ) {
                    entity.damage(DAMAGE_POTION_DAMAGE_WHEN_BLINDED)
                    entity.playSound(entity, Sound.PARTICLE_SOUL_ESCAPE, 10f, 1f)
                    entity.removePotionEffect(PotionEffectType.BLINDNESS)
                    entity.playSound(potionOwner, Sound.PARTICLE_SOUL_ESCAPE, 10f, 1f)
                    peopleBlindedByPotions.remove(entity.uniqueId)
                } else entity.damage(DAMAGE_POTION_DAMAGE)

                entity.playSound(entity, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 10f, 1f)
                entity.playSound(potionOwner, Sound.ENTITY_ARROW_HIT_PLAYER, 10f, 1f)
            }
        }

        makeDamageAreaEffect(potion.location)
        potionOwner.playSound(potionOwner, Sound.BLOCK_FIRE_EXTINGUISH, 10f, 1f)
    }

    /**
     * Activates the blindness potion area effect.
     * @param potion the potion that will spawn a blindness cloud.
     */
    private fun activateBlindnessAreaEffect(potion: Entity, location: Location) {
        potion.world.spawn(location, AreaEffectCloud::class.java).let { cloud ->
            cloud.duration = BLINDNESS_POTION_WAIT_TIME
            cloud.radius = BLINDNESS_POTION_AREA.toFloat()
            cloud.particle = Particle.DRAGON_BREATH
            cloud.waitTime = 0
            ((potion as? ThrowableProjectile)?.shooter as? Player)?.let { player ->
                cloud.ownerUniqueId = player.uniqueId
                player.playSound(player, Sound.ENTITY_SPLASH_POTION_BREAK, 10f, 1f)
                createBlindnessTickTask(
                    potion = cloud,
                    potionOwner = player
                )
            }
        }
    }

    private fun createBlindnessTickTask(
        potion: AreaEffectCloud,
        potionOwner: Player
    ) = runLater(
        plugin = plugin,
        ticksDelay = BLINDNESS_POTION_WAIT_TIME.toLong()
    ) {
        val hitEntities = BLINDNESS_POTION_AREA.let {
            potion.location.getNearbyEntities(it, it, it)
        }

        for (entity in hitEntities) {
            if (entity is Player && entity.uniqueId != potionOwner.uniqueId) {
                peopleBlindedByPotions[entity.uniqueId]?.add(potion.uniqueId) ?: run {
                    peopleBlindedByPotions[entity.uniqueId] = mutableListOf(potion.uniqueId)
                }

                entity.addPotionEffect(
                    PotionEffect(PotionEffectType.BLINDNESS, BLINDNESS_POTION_DURATION, BLINDNESS_POTION_AMPLIFIER)
                )
                entity.playSound(entity, Sound.PARTICLE_SOUL_ESCAPE, 10f, 1f)
                entity.playSound(potionOwner, Sound.BLOCK_MOSS_CARPET_HIT, 10f, 1f)
            }
        }

        makeBlindnessAreaEffect(potion.location)
        potionOwner.playSound(potionOwner, Sound.PARTICLE_SOUL_ESCAPE, 10f, 1f)

        runAsyncLater(plugin, BLINDNESS_POTION_DURATION.toLong()) {
            removePotionFromBlindedList(hitEntities, potion.uniqueId)
        }
    }

    private fun removePotionFromBlindedList(peopleToRemove: Collection<Entity>, potionToRemove: UUID) {
        for (entity in peopleToRemove) {
            if (entity is Player) {
                if ((peopleBlindedByPotions[entity.uniqueId]?.size ?: 0) <= 1) {
                    peopleBlindedByPotions.remove(entity.uniqueId)
                } else peopleBlindedByPotions[entity.uniqueId]?.remove(potionToRemove)
            }
        }
    }

    private fun activateHealAreaEffect(potion: Entity, location: Location) {
        potion.world.spawn(location, AreaEffectCloud::class.java).let { cloud ->
            cloud.duration = (HEAL_POTION_TICKS * HEAL_POTION_TICKS_DELAY + HEAL_POTION_WAIT_TIME).toInt()
            cloud.radius = HEAL_POTION_CLOUD_AREA.toFloat()
            cloud.particle = Particle.COMPOSTER
            cloud.waitTime = 0
            ((potion as? ThrowableProjectile)?.shooter as? Player)?.let { player ->
                cloud.ownerUniqueId = player.uniqueId
                player.playSound(player, Sound.ENTITY_SPLASH_POTION_BREAK, 10f, 1f)
                createHealingPotionEffectTask(
                    potion = cloud,
                    potionOwner = player
                )
            }
        }
    }

    private fun createHealingPotionEffectTask(
        potion: AreaEffectCloud,
        potionOwner: Player,
    ) = iterateRunLater(
        plugin = plugin,
        initialTicksDelay = HEAL_POTION_WAIT_TIME,
        iterationsTickDelay = HEAL_POTION_TICKS_DELAY,
        iterations = HEAL_POTION_TICKS
    )  { iterationsLeft ->
        val hitEntities = HEAL_POTION_CLOUD_AREA.let {
            potion.location.getNearbyEntities(it, it, it)
        }

        var isPlayerInArea = false

        // Heals player and knockback enemies
        for (entity in hitEntities) {
            if (entity.uniqueId == potionOwner.uniqueId) {
                HealPlayerUseCase(potionOwner, HEAL_POTION_HEAL_AMOUNT_PER_TICK)
                potionOwner.playSound(potionOwner, Sound.BLOCK_NOTE_BLOCK_CHIME, 10f, 1f)
                isPlayerInArea = true
            } else if (entity is Player) {
                val knockback = entity.location.toVector().subtract(potion.location.toVector()).normalize().let {
                    if (entity.isJumping) {
                        it.y = it.y.coerceAtLeast(0.5)
                    } else it.y = it.y.coerceAtLeast(0.1)
                    it.normalize()
                }
                entity.velocity = knockback.multiply(KNOCKBAK_HEAL_MULTIPLIER)
                potionOwner.playSound(potionOwner, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 10f, 1f)
                entity.playSound(entity, Sound.ENTITY_GENERIC_EXPLODE, 10f, 1f)
            }
        }

        // Knockbacks player if is the last tick
        if (iterationsLeft == 0 && isPlayerInArea) {
            val knockback = potionOwner.location.toVector().subtract(potion.location.toVector()).normalize().let {
                if (potionOwner.isJumping) {
                    it.y = it.y.coerceAtLeast(0.5)
                } else it.y = it.y.coerceAtLeast(0.1)
                it.normalize()
            }
            potionOwner.velocity = knockback.multiply(KNOCKBAK_HEAL_MULTIPLIER)
            potionOwner.playSound(potionOwner, Sound.ENTITY_GENERIC_EXPLODE, 10f, 1f)
        }

        makeHealAreaEffect(potion.location)
    }

    private fun makeDamageAreaEffect(location: Location) = runAsync(plugin = plugin){
        (DAMAGE_POTION_CLOUD_AREA / 3).let {
            location.world.spawnParticle(
                Particle.FALLING_LAVA,
                location,
                DAMAGE_POTION_FIRE_PARTICLE_QUANTITY,
                it,
                it,
                it,
            )
        }

        (DAMAGE_POTION_CLOUD_AREA/4).let {
            location.world.spawnParticle(
                Particle.DUST_PLUME,
                location,
                POTION_EXPLOSION_PARTICLE_QUANTITY,
                it,
                it,
                it,
                0.0
            )
        }
    }

    private fun makeBlindnessAreaEffect(location: Location) = runAsync(plugin = plugin) {
        (BLINDNESS_POTION_AREA / 3).let {
            location.world.spawnParticle(
                Particle.FALLING_OBSIDIAN_TEAR,
                location,
                BLINDNESS_POTION_PARTICLE_QUANTITY,
                it,
                it,
                it,
            )
        }

        (BLINDNESS_POTION_AREA/3).let {
            location.world.spawnParticle(
                Particle.DUST_PLUME,
                location,
                POTION_EXPLOSION_PARTICLE_QUANTITY,
                it,
                it,
                it,
                0.0
            )
        }
    }

    private fun makeHealAreaEffect(location: Location) = runAsync(plugin = plugin) {
        (HEAL_POTION_CLOUD_AREA / 2).let {
            location.world.spawnParticle(
                Particle.ENCHANTMENT_TABLE,
                location,
                HEAL_POTION_PARTICLE_QUANTITY,
                it,
                it,
                it,
            )
        }

        (HEAL_POTION_CLOUD_AREA / 5).let {
            location.world.spawnParticle(
                Particle.DUST_PLUME,
                location,
                POTION_EXPLOSION_PARTICLE_QUANTITY,
                it,
                it,
                it,
                0.0
            )
        }
    }
}