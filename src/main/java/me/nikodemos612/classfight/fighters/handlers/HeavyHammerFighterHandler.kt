package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.fighters.DefaultFighterHandler
import me.nikodemos612.classfight.utill.BounceProjectileOnHitUseCase
import me.nikodemos612.classfight.utill.HealPlayerUseCase
import me.nikodemos612.classfight.utill.RunInLineBetweenTwoLocationsUseCase
import me.nikodemos612.classfight.utill.cooldown.Cooldown
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.entity.*
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.roundToInt

private const val PRIMARY_ATTACK_COOLDONW = 750L
private const val HAMMER_DISTANCE_1_FROM_PLAYER = 4.5
private const val HAMMER_DISTANCE_2_FROM_PLAYER = 4.0
private const val HAMMER_DISTANCE_3_FROM_PLAYER = 3.5
private const val HAMMER_DISTANCE_4_FROM_PLAYER = 3.0
private const val HAMMER_TASK_DELAY = 1L
private const val HAMMER_REMOVE_DELAY = 10L
private const val HAMMER_MOVEMENT_VELOCITY = 1
private const val HAMMER_PLAYER_MOVEMENT_VELOCITY = 0.1

private const val HAMMER_SLASH_RADIOS = 0.5
private const val HAMMER_SLASH_MAX_DISTANCE = 5
private const val HAMMER_SLASH_DAMAGE = 7.0
private const val HAMMER_SLASH_COOLDOWN = 5000L
private const val HAMMER_SLASH_MIN_FORCE = 0.2

private const val HAMMER_BONK_AREA = 2.0
private const val HAMMER_BONK_DAMAGE = 10.0
private const val HAMMER_BONK_COOLDOWN = 10000L
private const val HAMMER_BONK_MIN_FORCE = 2.0
private const val HAMMER_BONK_PARTICLE_QUANTITY = 150

private const val JAIL_NAME = "jail"
private const val JAIL_VELOCITY_MULTIPLIER = 2.0
private const val JAIL_COOLDOWN = 15000L
private const val JAIL_DURATION = 120
private const val JAIL_WAIT_TIME = 5
private const val JAILED_TASK_DELAY = 2L
private const val JAILED_AREA = 6.0
private const val JAILED_PUSH_FORCE_MULTIPLIER = 0.05
private const val JAIL_PROJECTILE_FRICTION = 0.1
private const val JAIL_HEAL_PER_PLAYER= 1.0
private const val JAIL_HEAL_DELAY = 10L
private const val JAILED_AREA_EFFECT = 5.0

private const val JAIL_DASH_COOLDONW = 17000L
private const val JAIL_DASH_MAX_DISTANCE_RADIOS = 20.0
private const val JAIL_DASH_MULTIPLIER = 0.15
private const val JAIL_DASH_BANG_PARTICLE_QUANTITY = 150
private const val JAIL_BANG_DURATION = 80
private const val JAIL_DASH_HOVER_EFFECT_DELAY = 4L

private const val RIGHT_CLICK_COOLDOWN = 10L

class HeavyHammerFighterHandler(private val plugin: Plugin): DefaultFighterHandler() {
    private val primaryAttackCooldown = Cooldown()
    private val hammerFromPlayers = HashMap<UUID, Hammer>()
    private val playerHammerDistance = HashMap<UUID, Double>()
    private data class Hammer(
        val hammerEntity: ArmorStand,
        val hammerTask: Int
    )

    private val playerSlashCooldown = Cooldown()
    private val playersOnHammerSlash = HashMap<UUID, SlashArgs>()
    private val playersSlashedByPlayer = HashMap<UUID, MutableList<UUID>>()
    private data class SlashArgs(
        val startOfTheSlashLocation: Location,
        var lastDistanceToStart: Double
    )

    private val playerHammerBonkCooldown = Cooldown()

    private val rightClickCooldown = Cooldown()
    private val jailCooldown = Cooldown()
    private val jailDashCooldown = Cooldown()
    private val listOfJailedFighterPlayers = mutableListOf<UUID>()

    override val fighterTeamName = "hammer"

    override fun resetInventory(player: Player) {
        player.inventory.clear()
        player.inventory.setItem(0, ItemStack(Material.STICK))
        player.inventory.setItem(1, ItemStack(Material.STICK))
        player.inventory.setItem(2, ItemStack(Material.STICK))
        player.inventory.setItem(3, ItemStack(Material.STICK))
        player.inventory.setItem(5, ItemStack(Material.IRON_SWORD))
        player.inventory.setItem(6, ItemStack(Material.NETHERITE_AXE))
        player.inventory.setItem(7, ItemStack(Material.IRON_BARS))
        player.inventory.setItem(8, ItemStack(Material.ENDER_EYE))
        player.flySpeed = 0.1F
    }

    override fun resetCooldowns(player: Player) {
        player.resetCooldown()
        primaryAttackCooldown.resetCooldown(from = player.uniqueId)
        removeHammerTask(player)
        playerHammerDistance.remove(player.uniqueId)

        playerSlashCooldown.resetCooldown(from = player.uniqueId)
        playersOnHammerSlash.remove(player.uniqueId)
        playersSlashedByPlayer.remove(player.uniqueId)

        playerHammerBonkCooldown.resetCooldown(from = player.uniqueId)

        rightClickCooldown.resetCooldown(from = player.uniqueId)
        jailCooldown.resetCooldown(from = player.uniqueId)
        jailDashCooldown.resetCooldown(from = player.uniqueId)
        listOfJailedFighterPlayers.remove(player.uniqueId)
    }

    override fun onProjectileHit(event: ProjectileHitEvent) {
        if (event.entity.customName() == Component.text(JAIL_NAME)) {
            (event.entity.shooter as? Player)?.let { player ->
                val hitEntity = event.hitEntity

                if (hitEntity != null) {
                    spawnJail(hitEntity, player)
                } else if (event.hitBlockFace == BlockFace.UP) {
                    spawnJail(event.entity, player)
                } else {
                    BounceProjectileOnHitUseCase(event, JAIL_PROJECTILE_FRICTION)
                }
            }
        }
    }

    override fun onPlayerInteraction(event: PlayerInteractEvent) {
        when {
            event.action.isLeftClick -> handleLeftClick(event)
            event.action.isRightClick -> handleRightClick(event)
        }
    }

    override fun onItemHeldChange(event: PlayerItemHeldEvent) {
        val playerUUID = event.player.uniqueId
        when (val newSlot = event.newSlot) {
            0 -> playerHammerDistance[playerUUID] = HAMMER_DISTANCE_1_FROM_PLAYER
            1 -> playerHammerDistance[playerUUID] = HAMMER_DISTANCE_2_FROM_PLAYER
            2 -> playerHammerDistance[playerUUID] = HAMMER_DISTANCE_3_FROM_PLAYER
            3 -> playerHammerDistance[playerUUID] = HAMMER_DISTANCE_4_FROM_PLAYER
            else -> {
                if (newSlot == 8) {
                    event.player.inventory.heldItemSlot = 3
                    playerHammerDistance[playerUUID] = HAMMER_DISTANCE_4_FROM_PLAYER
                } else {
                    event.player.inventory.heldItemSlot = 0
                    playerHammerDistance[playerUUID] = HAMMER_DISTANCE_1_FROM_PLAYER
                }
            }
        }
    }

    private fun handleLeftClick(event: PlayerInteractEvent) {
        val player = event.player
        if (!primaryAttackCooldown.hasCooldown(player.uniqueId)) {
            primaryAttackCooldown.addCooldownToPlayer(player.uniqueId, PRIMARY_ATTACK_COOLDONW)
            player.setCooldown(Material.STICK, (PRIMARY_ATTACK_COOLDONW / 50).toInt())

            if (hammerFromPlayers[player.uniqueId] != null){
                removeHammerTask(player)
            } else spawnHammer(player = player)
        }
    }

    private fun spawnHammer(player: Player) {
        (player.world.spawnEntity(player.eyeLocation, EntityType.ARMOR_STAND) as? ArmorStand)?.let {
            it.isVisible = false
            it.isSmall = true
            createHammerTask(player = player, hammer = it)
        }
    }

    private fun createHammerTask(player: Player, hammer: ArmorStand) {
        val hammerTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(
            plugin,
            hammerTask(
                hammer = hammer,
                player = player
            ),
            0,
            HAMMER_TASK_DELAY
        )
        hammerFromPlayers[player.uniqueId] = Hammer(hammer, hammerTask)
    }

    private fun removeHammerTask(player: Player) {
        hammerFromPlayers[player.uniqueId]?.let { hammerArgs ->
            val scheduler = Bukkit.getScheduler()
            scheduler.cancelTask(hammerArgs.hammerTask)

            val hammerRemoveTask = scheduler.scheduleSyncRepeatingTask(
                plugin,
                hammerRemoveTask(
                    hammer = hammerArgs.hammerEntity,
                    player = player
                ),
                0,
                HAMMER_TASK_DELAY
            )

            scheduler.runTaskLater(
                plugin,
                Runnable {
                    scheduler.cancelTask(hammerRemoveTask)
                    hammerArgs.hammerEntity.remove()
                },
                HAMMER_REMOVE_DELAY
            )
        }

        hammerFromPlayers.remove(player.uniqueId)
    }

    private fun hammerTask(hammer: ArmorStand, player: Player) = Runnable {
        val world = hammer.world
        val hammerDistance = playerHammerDistance[player.uniqueId] ?: HAMMER_DISTANCE_1_FROM_PLAYER

        // Move the hammer
        val locationToMove = player.eyeLocation.toVector().add(
            player.eyeLocation.direction.normalize().multiply(hammerDistance)
        )
        val hammerMovementVector = locationToMove.subtract(hammer.location.toVector())
        hammer.velocity = player.velocity.add(hammerMovementVector.multiply(HAMMER_MOVEMENT_VELOCITY))
        hammer.location.direction = player.eyeLocation.direction

        // If the hammer can't move, move the player
        val normalizedLocationToMove = hammer.velocity.normalize().multiply(0.5).add(hammer.location.toVector())
            .toLocation(world)
        if (normalizedLocationToMove.block.type.isSolid) {
            if (hammer.velocity.length() >= HAMMER_BONK_MIN_FORCE) {
                makeHammerBonk(hammer.location, player)
            }

            hammer.velocity = Vector(0, 0, 0)
            player.velocity = player.velocity.add(
                hammerMovementVector.multiply(-1).multiply(HAMMER_PLAYER_MOVEMENT_VELOCITY)
            )
        } else {
            makePlayerSlash(hammer, player)
        }

        when {
            !playerHammerBonkCooldown.hasCooldown(player.uniqueId) &&
                    !playerSlashCooldown.hasCooldown(player.uniqueId) ->
                makeHammerBonkAndSlashParticle(hammer.location, hammer.world)

            !playerHammerBonkCooldown.hasCooldown(player.uniqueId) &&
                    playersOnHammerSlash[player.uniqueId] != null ->
                makeHammerBonkAndSlashingParticle(hammer.location, hammer.world)

            !playerHammerBonkCooldown.hasCooldown(player.uniqueId) ->
                makeHammerBonkParticle(hammer.location, hammer.world)

            !playerSlashCooldown.hasCooldown(player.uniqueId) ->
                makeHammerSlashParticle(hammer.location, hammer.world)

            playersOnHammerSlash[player.uniqueId] != null ->
                makeHammerSlashingParticle(hammer.location, hammer.world)

            else -> makeHammerParticle(hammer.location, hammer.world)
        }
    }

    private fun makePlayerSlash(hammer: ArmorStand, hammerOwner: Player) {
        val hammerOwnerUUID = hammerOwner.uniqueId
        val hammerSlashIntensity = hammer.velocity.subtract(hammerOwner.velocity).length()
        val isSlashing = isPlayerSlashing(
            hammerOwnerUUID = hammerOwnerUUID,
            hammer = hammer,
            slashIntensity = hammerSlashIntensity
        )

        if (isSlashing || isPlayerTryingToSlash(slashIntensity = hammerSlashIntensity, playerUUID = hammerOwnerUUID)) {
            val damagedEntities = HAMMER_SLASH_RADIOS.let {
                hammer.getNearbyEntities(it, it, it)
            }

            if (damagedEntities.isNotEmpty()) {
                if (!isSlashing) {
                    playerSlashCooldown.addCooldownToPlayer(hammerOwnerUUID, HAMMER_SLASH_COOLDOWN)
                    hammerOwner.setCooldown(Material.IRON_SWORD, (HAMMER_SLASH_COOLDOWN / 50).toInt())
                    playersOnHammerSlash[hammerOwnerUUID] = SlashArgs(hammer.location, 0.0)
                }

                val listOfSlashed = playersSlashedByPlayer[hammerOwnerUUID].orEmpty().toMutableList()
                for (entity in damagedEntities) {
                    if (entity is Player && entity.shouldBeDamagedByHammer(hammerOwnerUUID, listOfSlashed)) {
                        entity.damage(HAMMER_SLASH_DAMAGE)
                        entity.playSound(entity, Sound.BLOCK_ANVIL_BREAK, 10f, 1f)
                        listOfSlashed.add(entity.uniqueId)
                    }
                }
                playersSlashedByPlayer[hammerOwnerUUID] = listOfSlashed

                hammerOwner.playSound(hammerOwner, Sound.BLOCK_ANVIL_PLACE, 10f, 1f)
            }
        }
    }

    private fun isPlayerSlashing(
        hammerOwnerUUID: UUID,
        hammer: ArmorStand,
        slashIntensity: Double
    ) : Boolean {
        playersOnHammerSlash[hammerOwnerUUID]?.let { slashArgs ->
            val newDistance = hammer.location.distance(slashArgs.startOfTheSlashLocation)
            if (
                isPlayerStillSlashing(
                    playerUUID = hammerOwnerUUID,
                    hammerDistanceToStart = newDistance,
                    hammerOldDistanceToStart = slashArgs.lastDistanceToStart,
                    slashIntensity = slashIntensity
                )
            ) {
                slashArgs.lastDistanceToStart = newDistance
                return true
            } else {
                playersOnHammerSlash.remove(hammerOwnerUUID)
                playersSlashedByPlayer.remove(hammerOwnerUUID)
            }
        }

        return false
    }

    private fun isPlayerTryingToSlash(playerUUID: UUID, slashIntensity: Double) =
        slashIntensity >= HAMMER_SLASH_MIN_FORCE && !playerSlashCooldown.hasCooldown(playerUUID)


    private fun isPlayerStillSlashing(
        playerUUID: UUID,
        hammerDistanceToStart: Double,
        hammerOldDistanceToStart: Double,
        slashIntensity: Double
    ): Boolean {
        return playerSlashCooldown.hasCooldown(playerUUID) &&
                hammerDistanceToStart > hammerOldDistanceToStart &&
                hammerDistanceToStart <= HAMMER_SLASH_MAX_DISTANCE &&
                slashIntensity >= HAMMER_SLASH_MIN_FORCE
    }

    private fun Player.shouldBeDamagedByHammer(hammerOwnerUUID: UUID, listOfSlashedPlayers: List<UUID>): Boolean =
        this.uniqueId != hammerOwnerUUID && !listOfSlashedPlayers.contains(this.uniqueId)

    private fun makeHammerBonk(hammerLocation: Location, hammerOwner: Player) {
        if (!playerHammerBonkCooldown.hasCooldown(hammerOwner.uniqueId)) {
            playerHammerBonkCooldown.addCooldownToPlayer(hammerOwner.uniqueId, HAMMER_BONK_COOLDOWN)
            hammerOwner.setCooldown(Material.NETHERITE_AXE, (HAMMER_BONK_COOLDOWN / 50).toInt())

            val entityDamaged = HAMMER_BONK_AREA.let {
                hammerLocation.getNearbyEntities(it, it, it)
            }

            for (entity in entityDamaged) {
                if (entity is Player && entity.uniqueId != hammerOwner.uniqueId) {
                    entity.damage(HAMMER_BONK_DAMAGE)
                    entity.velocity = entity.velocity.add(Vector(0, 1, 0))
                    entity.playSound(entity, Sound.BLOCK_ANVIL_HIT, 10f, 1f)
                }
            }

            hammerOwner.playSound(hammerOwner, Sound.BLOCK_ANVIL_LAND, 10f, 1f)
            makeHammerBonkExplosionParticles(hammerLocation)
        }
    }

    private fun hammerRemoveTask(hammer: ArmorStand, player: Player) = Runnable {
        val world = hammer.world

        // Move the hammer
        val hammerMovementVector = player.location.toVector().subtract(hammer.location.toVector())
        hammer.velocity = player.velocity.add(hammerMovementVector.multiply(HAMMER_MOVEMENT_VELOCITY))
        hammer.location.direction = player.eyeLocation.direction

        // If the hammer can't move, move the player
        val normalizedLocationToMove = hammer.velocity.normalize().multiply(0.6).add(hammer.location.toVector())
            .toLocation(world)
        if (normalizedLocationToMove.block.type.isSolid) {
            hammer.velocity = Vector(0,0,0)
            player.velocity = player.velocity.add(
                hammerMovementVector.multiply(-1).multiply(HAMMER_PLAYER_MOVEMENT_VELOCITY)
            )
        }
    }

    private fun handleRightClick(event: PlayerInteractEvent) {
        val player = event.player

        if (!rightClickCooldown.hasCooldown(player.uniqueId)) {
            rightClickCooldown.addCooldownToPlayer(player.uniqueId, RIGHT_CLICK_COOLDOWN)

            val jail = player.getNearbyEntities(JAILED_AREA, JAILED_AREA, JAILED_AREA).filter {
                it is AreaEffectCloud &&
                it.ownerUniqueId == player.uniqueId &&
                it.location.distance(player.location) < it.radius + 0.5
            }.let {
                if (it.isNotEmpty())
                    it[0]
                else null
            }

            if (jail != null && listOfJailedFighterPlayers.contains(player.uniqueId)) {
                listOfJailedFighterPlayers.remove(player.uniqueId)
                player.playSound(player, Sound.BLOCK_CHAIN_BREAK, 10f, 1f)
            } else if (!jailCooldown.hasCooldown(player.uniqueId)) {
                throwJailProjectile(player)
                jailCooldown.addCooldownToPlayer(player.uniqueId, JAIL_COOLDOWN)
                player.setCooldown(
                    player.inventory.getItem(7)?.type ?: Material.BEDROCK,
                    (JAIL_COOLDOWN / 50).toInt()
                )
            } else if (!jailDashCooldown.hasCooldown(player.uniqueId)) {
                if (dashToJail(player)) {
                    jailDashCooldown.addCooldownToPlayer(player.uniqueId, JAIL_DASH_COOLDONW)
                    player.inventory.getItem(8)?.type?.let {
                        player.setCooldown(it, (JAIL_DASH_COOLDONW / 50).toInt())
                    }
                }
            }
        }
    }

    private fun throwJailProjectile(player: Player) {
        player.launchProjectile(
            Snowball::class.java,
            player.location.direction.multiply(JAIL_VELOCITY_MULTIPLIER)
        ).let {
            it.customName(Component.text(JAIL_NAME))
            it.shooter = player
        }
    }


    private fun spawnJail(entity: Entity, owner: Player) {
        val jail = spawnJailedArea(entity, owner.uniqueId)
        val jailLocation = jail.location
        val world = jail.world

        owner.playSound(owner, Sound.ENTITY_EVOKER_PREPARE_WOLOLO, 10f, 5f)

        Bukkit.getScheduler().runTaskLater(
            plugin,
            Runnable {
                val jailedEntities = JAILED_AREA_EFFECT.let {
                    jailLocation.getNearbyEntities(it, it, it)
                }
                val jailedPlayers = mutableListOf<Player>()

                for (player in jailedEntities) {
                    if (player is Player) {
                        makeJailPushParticlesLine(
                            location1 = player.location,
                            location2 = jailLocation,
                            world = world
                        )

                        player.playSound(player, Sound.BLOCK_CHAIN_PLACE, 10f, 1f)

                        if (player.uniqueId == owner.uniqueId) {
                            listOfJailedFighterPlayers.add(owner.uniqueId)
                        }

                        jailedPlayers.add(player)
                    }
                }

                if (jailedPlayers.isNotEmpty()) {
                    createJailedPlayersTask(
                        jailedPlayers = jailedPlayers,
                        jail = jail,
                        player = owner
                    )
                }
            },
            JAIL_WAIT_TIME.toLong()
        )
    }

    private fun spawnJailedArea(entity: Entity, ownerUUID: UUID): AreaEffectCloud =
        entity.world.spawn(entity.location, AreaEffectCloud::class.java).let { cloud ->
            cloud.radius = JAILED_AREA.toFloat()
            cloud.duration = JAIL_DURATION
            cloud.waitTime = JAIL_WAIT_TIME
            cloud.setParticle(Particle.DUST_COLOR_TRANSITION, Particle.DustTransition(Color.WHITE, Color.AQUA, 1F))
            cloud.customName(Component.text(JAIL_NAME))
            cloud.ownerUniqueId = ownerUUID
            cloud
        }


    private fun createJailedPlayersTask(
        jailedPlayers: List<Player>,
        jail: AreaEffectCloud,
        player: Player
    ) {
        val jailTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(
            plugin,
            jailPlayersTask(
                jailedPlayers = jailedPlayers,
                jail = jail
            ),
            0,
            JAILED_TASK_DELAY
        )
        val healTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(
            plugin,
            healPlayerTask(
                jailedPlayers= jailedPlayers,
                player = player,
                jailLocation = jail.location
            ),
            0,
            JAIL_HEAL_DELAY
        )
        val jailHoverTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(
            plugin,
            createDashHoverEffectTask(
                player = player,
                jail = jail
            ),
            0,
            JAIL_DASH_HOVER_EFFECT_DELAY
        )

        Bukkit.getScheduler().runTaskLater(
            plugin,
            Runnable {
                val scheduler = Bukkit.getScheduler()
                scheduler.cancelTask(jailTask)
                scheduler.cancelTask(healTask)
                scheduler.cancelTask(jailHoverTask)
                listOfJailedFighterPlayers.remove(player.uniqueId)
                player.playSound(player, Sound.ENTITY_SPLASH_POTION_BREAK, 10f, 1f)

                for (jailedPlayer in jailedPlayers) {
                    if (jailedPlayer.uniqueId != player.uniqueId)
                        jailedPlayer.playSound(jailedPlayer, Sound.BLOCK_CHAIN_BREAK, 10f, 1f)
                }
            },
            JAIL_DURATION.toLong()
        )
    }

    private fun jailPlayersTask(jailedPlayers: List<Player>, jail: AreaEffectCloud) = Runnable {
        val world = jail.world
        val jailLocation = jail.location
        for (player in jailedPlayers) {
            val playerLocation = player.location

            if (
                !player.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE) &&
                (player.uniqueId != jail.ownerUniqueId || listOfJailedFighterPlayers.contains(player.uniqueId))
            ) {
                if (playerLocation.distance(jailLocation) > jail.radius) {
                    val pushVector = jailLocation.toVector().subtract(playerLocation.toVector()).multiply(
                        JAILED_PUSH_FORCE_MULTIPLIER
                    )
                    player.velocity = player.velocity.add(pushVector)

                    makeJailPushParticlesLine(
                        jailLocation,
                        playerLocation,
                        world
                    )
                    player.playSound(player, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 10f, 10f)
                } else {
                    makeJailedParticlesLine(
                        location1 = player.location,
                        location2 = jailLocation,
                        world = world
                    )
                }
            }
        }
    }

    private fun healPlayerTask(jailedPlayers: List<Player>, player: Player, jailLocation: Location) = Runnable {
        var jailedPlayersQuantity = 0

        for (jailedPlayer in jailedPlayers) {
            if (jailedPlayer.uniqueId != player.uniqueId) {
                makeHealEffectParticles(
                    jailedPlayer.location,
                    jailLocation,
                    jailLocation.world,
                    1f
                )

                jailedPlayer.playSound(jailedPlayer, Sound.PARTICLE_SOUL_ESCAPE, 10f, 1f)
                jailedPlayersQuantity++
            }
        }

        if (HealPlayerUseCase(player, jailedPlayersQuantity * JAIL_HEAL_PER_PLAYER)) {
            makeHealEffectParticles(
                player.location,
                jailLocation,
                jailLocation.world,
                jailedPlayers.size.toFloat()
            )
            player.playSound(player, Sound.BLOCK_AMETHYST_CLUSTER_STEP, 10f, 1f)
        }
    }

    private fun createDashHoverEffectTask(player: Player, jail: AreaEffectCloud) = Runnable {
        val targetIsThisJail = {
            player.getTargetBlockExact(JAIL_DASH_MAX_DISTANCE_RADIOS.roundToInt())?.location
                ?.getNearbyEntities(4.0, 4.0, 4.0)
                ?.any { it is AreaEffectCloud && it.uniqueId == jail.uniqueId } ?: false
        }

        if (
            !jailDashCooldown.hasCooldown(player.uniqueId) &&
            targetIsThisJail() &&
            !listOfJailedFighterPlayers.contains(player.uniqueId)
        ) {
            jail.setParticle(Particle.DUST_COLOR_TRANSITION, Particle.DustTransition(Color.BLACK, Color.WHITE, 1F))
        } else {
            jail.setParticle(Particle.DUST_COLOR_TRANSITION, Particle.DustTransition(Color.WHITE, Color.AQUA, 1F))
        }
    }

    private fun dashToJail(player: Player): Boolean {
        val targetJail = player.getTargetBlockExact(JAIL_DASH_MAX_DISTANCE_RADIOS.roundToInt())?.location
            ?.getNearbyEntities(4.0, 4.0, 4.0)
            ?.firstOrNull { it is AreaEffectCloud && it.ownerUniqueId == player.uniqueId } as? AreaEffectCloud

        if (
            targetJail != null
        ) {
            makeBangEffectParticles(targetJail.location)

            player.playSound(player, Sound.PARTICLE_SOUL_ESCAPE, 10f, 1f)
            player.playSound(player, Sound.ENTITY_FISHING_BOBBER_THROW, 10f, 1f)

            val playersJailed = JAILED_AREA.let { area ->
                targetJail.location.getNearbyEntities(area, area, area)
            }

            for (playerJailed in playersJailed) {
                if (playerJailed is Player) {
                    playerJailed.addPotionEffect(
                        PotionEffect(
                            PotionEffectType.BLINDNESS,
                            JAIL_BANG_DURATION,
                            1
                        )
                    )

                    player.playSound(player, Sound.PARTICLE_SOUL_ESCAPE, 10f, 1f)
                    player.playSound(player, Sound.BLOCK_CHAIN_HIT, 10f, 1f)
                }
            }

            val dash = targetJail.location.toVector().subtract(player.location.toVector()).let {
                it.multiply(JAIL_DASH_MULTIPLIER)
                it.y = it.y.coerceAtLeast(0.0) + 0.5
                it
            }
            player.velocity = dash

            makeJailDashParticlesLine(
                location1 = player.location,
                location2 = targetJail.location,
                world = player.world
            )

            return true
        }

        return false
    }

    private fun makeHammerBonkAndSlashParticle(location: Location, world: World) {
        world.spawnParticle(
            Particle.DUST_COLOR_TRANSITION,
            location.x,
            location.y,
            location.z,
            1,
            Particle.DustTransition(Color.RED, Color.BLACK,2f)
        )
    }

    private fun makeHammerBonkAndSlashingParticle(location1: Location, world: World) {
        world.spawnParticle(
            Particle.DUST_COLOR_TRANSITION,
            location1.x,
            location1.y,
            location1.z,
            1,
            Particle.DustTransition(Color.RED, Color.AQUA,2f)
        )
    }

    private fun makeHammerBonkParticle(location1: Location, world: World) {
        world.spawnParticle(
            Particle.DUST_COLOR_TRANSITION,
            location1.x,
            location1.y,
            location1.z,
            1,
            Particle.DustTransition(Color.RED, Color.WHITE,2f)
        )
    }

    private fun makeHammerSlashParticle(location1: Location, world: World) {
        world.spawnParticle(
            Particle.DUST_COLOR_TRANSITION,
            location1.x,
            location1.y,
            location1.z,
            1,
            Particle.DustTransition(Color.BLACK, Color.GRAY,2f)
        )
    }

    private fun makeHammerSlashingParticle(location1: Location, world: World) {
        world.spawnParticle(
            Particle.DUST_COLOR_TRANSITION,
            location1.x,
            location1.y,
            location1.z,
            1,
            Particle.DustTransition(Color.AQUA, Color.GRAY,2f)
        )
    }

    private fun makeHammerParticle(location: Location, world: World) {
        world.spawnParticle(
            Particle.REDSTONE,
            location.x,
            location.y,
            location.z,
            1,
            Particle.DustOptions(Color.WHITE, 2f)
        )
    }

    private fun makeHammerBonkExplosionParticles(location: Location) {
        (HAMMER_BONK_AREA / 2).let {
            location.world.spawnParticle(
                Particle.CRIT,
                location,
                HAMMER_BONK_PARTICLE_QUANTITY,
                it,
                it,
                it,
            )
        }
    }

    private fun makeJailedParticlesLine(
        location1: Location,
        location2: Location,
        world: World,
    ) = RunInLineBetweenTwoLocationsUseCase(
        location1 = location1,
        location2 = location2,
        stepSize = 0.5,
        stepFun =  { location : Vector ->
            world.spawnParticle(
                Particle.REDSTONE,
                location.x,
                location.y,
                location.z,
                1,
                Particle.DustOptions(Color.BLACK, 1F)
            )
        },
    )

    private fun makeJailPushParticlesLine(
        location1 : Location,
        location2: Location,
        world: World
    ) = RunInLineBetweenTwoLocationsUseCase(
        location1 = location1,
        location2 = location2,
        stepSize = 0.5,
        stepFun =  { location : Vector ->
            world.spawnParticle(
                Particle.DUST_COLOR_TRANSITION,
                location.x,
                location.y,
                location.z,
                1,
                Particle.DustTransition(Color.RED, Color.BLACK, 2f)
            )
        }
    )

    private fun makeJailDashParticlesLine(
        location1: Location,
        location2: Location,
        world: World
    ) = RunInLineBetweenTwoLocationsUseCase(
        location1 = location1,
        location2 = location2,
        stepFun = { location: Vector->
            world.spawnParticle(
                Particle.DUST_COLOR_TRANSITION,
                location.toLocation(world),
                1,
                Particle.DustTransition(Color.RED, Color.WHITE, 3f)
            )
        },
        stepSize = 0.5,
    )

    private fun makeBangEffectParticles(
        location: Location
    ) {
        (JAILED_AREA / 2).let {
            location.world.spawnParticle(
                Particle.REDSTONE,
                location,
                JAIL_DASH_BANG_PARTICLE_QUANTITY,
                it,
                it,
                it,
                Particle.DustOptions(Color.BLACK, 3f),
            )
        }
    }

    private fun makeHealEffectParticles(
        location1: Location,
        location2: Location,
        world: World,
        size: Float
    ) = RunInLineBetweenTwoLocationsUseCase(
        location1 = location1,
        location2 = location2,
        stepFun = { location: Vector->
            world.spawnParticle(
                Particle.DUST_COLOR_TRANSITION,
                location.toLocation(world),
                1,
                Particle.DustTransition(Color.GREEN, Color.WHITE, size)
            )
        },
        stepSize = 0.5,
    )
}