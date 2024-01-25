package me.nikodemos612.classfight.fighters.handlers

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
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.*


private const val PRIMARY_ATTACK_COOLDONW = 1000L
private const val PRIMARY_ATTACK_DISTANCE = 10
private const val PRIMARY_ATTACK_DAMAGE_DELAY = 1
private const val PRIMARY_ATTACK_DISTANCE_BETWEEN_FANGS = 0.75

private const val JAIL_NAME = "jail"
private const val JAIL_VELOCITY_MULTIPLIER = 2.0
private const val JAIL_COOLDOWN = 10000L
private const val JAIL_DURATION = 120
private const val JAIL_WAIT_TIME = 5
private const val JAILED_TASK_DELAY = 2L
private const val JAILED_AREA = 6.0
private const val JAILED_PUSH_FORCE_MULTIPLIER = 0.05
private const val JAIL_PROJECTILE_FRICTION = 0.25
private const val JAIL_HEAL_PER_PLAYER= 1.0
private const val JAIL_HEAL_DELAY = 10L

private const val JAIL_DASH_COOLDONW = 15000L
private const val JAIL_DASH_MAX_DISTANCE_RADIOS = 20.0
private const val JAIL_DASH_MULTIPLIER = 0.15
private const val JAIL_DASH_BANG_PARTICLE_QUANTITY = 1000
private const val JAIL_BANG_DURATION = 40

private const val RIGHT_CLICK_COOLDOWN = 10L

object FangsPublicArgs {
    const val FANG_DAMAGE = 6.0
}

class FangsFighterHandler(private val plugin: Plugin): DefaultFighterHandler() {
    private val primaryAttackCooldown = Cooldown()
    private val fangsCooldown = Cooldown()
    private val rightClickCooldown = Cooldown()
    private val jailCooldown = Cooldown()
    private val jailDashCooldown = Cooldown()
    private val listOfCanceledJailFighterPlayers = mutableListOf<UUID>()

    override val fighterTeamName = "fangs"

    override fun resetInventory(player: Player) {
        player.inventory.clear()
        player.inventory.setItem(0, ItemStack(Material.STICK))
        player.inventory.setItem(7, ItemStack(Material.IRON_BARS))
        player.inventory.setItem(8, ItemStack(Material.ENDER_EYE))
    }

    override fun resetCooldowns(player: Player) {
        player.resetCooldown()
        jailCooldown.resetCooldown(from = player.uniqueId)
        jailDashCooldown.resetCooldown(from = player.uniqueId)
        fangsCooldown.resetCooldown(from = player.uniqueId)
        jailDashCooldown.resetCooldown(from = player.uniqueId)
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

    private fun handleLeftClick(event: PlayerInteractEvent) {
        val player = event.player
        if (!primaryAttackCooldown.hasCooldown(player.uniqueId)) {
            spawnFangLine(player)

            primaryAttackCooldown.addCooldownToPlayer(player.uniqueId, PRIMARY_ATTACK_COOLDONW)
            player.inventory.getItem(0)?.type?.let { player.setCooldown(it, (PRIMARY_ATTACK_COOLDONW / 50).toInt()) }
        }
    }
    private fun spawnFang(location: Location, player: Player) {
        player.world.spawnEntity(
            location,
            EntityType.EVOKER_FANGS
        ).let {
            (it as? EvokerFangs)?.attackDelay = PRIMARY_ATTACK_DAMAGE_DELAY
        }
    }

    private fun spawnFangLine(player: Player) {
        val distanceToWall =
            player.getTargetBlockExact(PRIMARY_ATTACK_DISTANCE)?.location?.distance(player.location)?.dec() ?:
            PRIMARY_ATTACK_DISTANCE.toDouble()

        RunInLineBetweenTwoLocationsUseCase(
            location1 = player.location.add(player.location.direction),
            location2 = player.location.add(player.location.direction.multiply(distanceToWall)),
            stepSize = PRIMARY_ATTACK_DISTANCE_BETWEEN_FANGS,
            stepFun = { location: Vector ->
                spawnFang(Location(player.world, location.x, location.y, location.z), player = player)
            }
        )
    }

    private fun handleRightClick(event: PlayerInteractEvent) {
        val player = event.player

        if (!rightClickCooldown.hasCooldown(player.uniqueId)) {
            rightClickCooldown.addCooldownToPlayer(player.uniqueId, RIGHT_CLICK_COOLDOWN)

            val jail = player.getNearbyEntities(JAILED_AREA, JAILED_AREA, JAILED_AREA).filter {
                it is AreaEffectCloud &&
                it.ownerUniqueId == player.uniqueId &&
                it.location.distance(player.location) < JAILED_AREA
            }.let {
                if (it.isNotEmpty())
                    it[0]
                else null
            }

            if (jail != null && !listOfCanceledJailFighterPlayers.contains(player.uniqueId)) {
                listOfCanceledJailFighterPlayers.add(player.uniqueId)
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

        Bukkit.getScheduler().runTaskLater(
            plugin,
            Runnable {
                val jailedPlayers = JAILED_AREA.let {
                    jailLocation.getNearbyEntities(it, it, it).filterIsInstance<Player>()
                }

                for (player in jailedPlayers) {
                    makeJailPushParticlesLine(
                        location1 = player.location,
                        location2 = jailLocation,
                        world = world
                    )
                }

                createJailedPlayersTask(
                    jailedPlayers = jailedPlayers,
                    jail = jail,
                    player = owner
                )
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
                jailedPlayers= jailedPlayers.filter { it.uniqueId != player.uniqueId },
                player = player,
                jailLocation = jail.location
            ),
            0,
            JAIL_HEAL_DELAY
        )


        Bukkit.getScheduler().runTaskLater(
            plugin,
            Runnable {
                val scheduler = Bukkit.getScheduler()
                scheduler.cancelTask(jailTask)
                scheduler.cancelTask(healTask)
                listOfCanceledJailFighterPlayers.remove(player.uniqueId)
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
                !listOfCanceledJailFighterPlayers.contains(player.uniqueId)
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
        val healthToAdd = jailedPlayers.size * JAIL_HEAL_PER_PLAYER
        if (HealPlayerUseCase(player, healthToAdd)) {

            for (jailedPlayer in jailedPlayers) {
                makeHealEffectParticles(
                    jailedPlayer.location,
                    jailLocation,
                    jailLocation.world,
                    1f
                )
            }

            makeHealEffectParticles(
                player.location,
                jailLocation,
                jailLocation.world,
                jailedPlayers.size.toFloat()
            )
        }
    }

    private fun dashToJail(player: Player): Boolean {
        val entities = JAIL_DASH_MAX_DISTANCE_RADIOS.let { player.getNearbyEntities(it, it, it) }

        for (entity in entities) {
            if (
                entity is AreaEffectCloud &&
                entity.ownerUniqueId == player.uniqueId &&
                player.hasLineOfSight(entity)
            ) {
                makeBangEffectParticles(entity.location)

                val playersJailed = JAILED_AREA.let { area ->
                    entity.location.getNearbyEntities(area, area, area).filterIsInstance<Player>()
                }

                for (playerJailed in playersJailed) {
                    playerJailed.addPotionEffect(
                        PotionEffect(
                                PotionEffectType.BLINDNESS,
                                JAIL_BANG_DURATION,
                               1
                        )
                    )
                }

                val dash = entity.location.toVector().subtract(player.location.toVector()).let {
                    it.multiply(JAIL_DASH_MULTIPLIER)
                    it.y = it.y.coerceAtLeast(0.0) + 0.5
                    it
                }
                player.velocity = dash

                makeJailDashParticlesLine(
                    location1 = player.location,
                    location2 = entity.location,
                    world = player.world
                )

                return true
            }
        }

        return false
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