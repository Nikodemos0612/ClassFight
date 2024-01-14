package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.utill.BounceProjectileOnHitUseCase
import me.nikodemos612.classfight.utill.MakeLineBetweenTwoLocationsUseCase
import me.nikodemos612.classfight.utill.player.Cooldown
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.BlockFace
import org.bukkit.entity.*
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import kotlin.math.roundToInt

private const val TEAM_NAME = "fangs"

private const val PRIMARY_ATTACK_COOLDONW = 2000L
private const val PRIMARY_ATTACK_DISTANCE = 5
private const val PRIMARY_ATTACK_FANGS_COOLDOWN = 200L
private const val PRIMARY_ATTACK_SLOW_EFFECT_AMPLIFIER = 1
private const val PRIMARY_ATTACK_SLOW_FALLING_AMPLIFIER = 50
private const val PRIMARY_ATTACK_JUMP_AMPLIFIER = 2
private const val PRIMARY_ATTACK_DAMAGE_DELAY = 5

private const val JAIL_VELOCITY_MULTIPLIER = 2.0
private const val JAIL_COOLDOWN = 10000L
private const val JAIL_EFFECT_AREA = 4F
private const val JAIL_DURATION = 120
private const val JAIL_WAIT_TIME = 5
private const val JAIL_SLOW_AMPLIFIER = 0
private const val JAIL_PROJECTILE_FRICTION = 0.25
private const val JAIL_DEFAULT_HEAL = 10.0
private const val JAIL_HEAL_PER_JAILED = 2
private const val BONUS_JAIL_HEAL_TIME = 80
private const val JAIL_DASH_COOLDONW = 15000L
private const val JAIL_DASH_MAX_DISTANCE_RADIOS = 20.0
private const val JAIL_DASH_MULTIPLIER = 0.15

object FangsPublicArgs {
    const val JAIL_NAME = "jail"
    val JAIL_EFFECT: PotionEffectType = PotionEffectType.WEAKNESS
    const val JAILED_AREA = 6.0
    const val JAIL_EFFECT_DURATION = 5
    const val FANG_DAMAGE = 4.0
}

class FangsFighterHandler: DefaultFighterHandler(){
    private val primaryAttackCooldown = Cooldown()
    private val playersOnPrimaryAttack = mutableListOf<UUID>()
    private val fangsCooldown = Cooldown()
    private val jailCooldown = Cooldown()
    private val jailDashCooldown = Cooldown()

    override fun canHandle(teamName: String): Boolean = teamName == TEAM_NAME

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
        playersOnPrimaryAttack.remove(player.uniqueId)
        jailDashCooldown.resetCooldown(from = player.uniqueId)
    }

    override fun onProjectileHit(event: ProjectileHitEvent) {
        if (event.entity.customName() == Component.text(FangsPublicArgs.JAIL_NAME)) {
            (event.entity.shooter as? Player)?.let { player ->
                val hitEntity = event.hitEntity

                if (hitEntity != null) {
                    spawnJailAreaEffect(hitEntity)
                    spawnJailedArea(hitEntity, player.uniqueId)
                } else if (event.hitBlockFace == BlockFace.UP) {
                    spawnJailAreaEffect(event.entity)
                    spawnJailedArea(event.entity, player.uniqueId)
                } else {
                    BounceProjectileOnHitUseCase(event, JAIL_PROJECTILE_FRICTION)
                }
            }
        }
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (playersOnPrimaryAttack.contains(player.uniqueId) && !fangsCooldown.hasCooldown(player.uniqueId)) {
            val entity = player.getTargetEntity(PRIMARY_ATTACK_DISTANCE)
            val distanceToPlayer = entity?.location?.distance(player.location) ?: PRIMARY_ATTACK_DISTANCE.toDouble()

            player.world.spawnEntity(
                   player.eyeLocation.add(player.eyeLocation.direction.multiply(
                           distanceToPlayer
                   )).let {
                        it.y -= 0.5
                        it
                    },
                    EntityType.EVOKER_FANGS
            ) .let {
                (it as? EvokerFangs)?.attackDelay = PRIMARY_ATTACK_DAMAGE_DELAY
             }

             player.world.spawnEntity(
                    player.eyeLocation.add(player.eyeLocation.direction.multiply(
                            distanceToPlayer + 1.25
                    )).let {
                        it.y -= 0.5
                        it
                    },
            EntityType.EVOKER_FANGS
            ) .let {
                (it as? EvokerFangs)?.attackDelay = PRIMARY_ATTACK_DAMAGE_DELAY
            }

            fangsCooldown.addCooldownToPlayer(player.uniqueId, PRIMARY_ATTACK_FANGS_COOLDOWN)
        }
    }

    override fun onPlayerInteraction(event: PlayerInteractEvent) {
        when {
            event.action.isRightClick -> handleRightClick(event)
            event.action.isLeftClick -> handleLeftClick(event)
        }
    }

    private fun handleRightClick(event: PlayerInteractEvent) {
        val player = event.player
        if (!jailCooldown.hasCooldown(player.uniqueId)) {
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

    private fun handleLeftClick(event: PlayerInteractEvent) {
        val player = event.player
        if (!primaryAttackCooldown.hasCooldown(player.uniqueId)) {
            if (playersOnPrimaryAttack.contains(player.uniqueId)) {
                deactivatePrimaryAttack(player)
            } else {
               activatePrimaryAttack(player)
            }

            primaryAttackCooldown.addCooldownToPlayer(player.uniqueId, PRIMARY_ATTACK_COOLDONW)
            player.inventory.getItem(0)?.type?.let { player.setCooldown(it, (PRIMARY_ATTACK_COOLDONW/50).toInt()) }
        }
    }

    private fun activatePrimaryAttack(player: Player) {
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.SLOW,
                PotionEffect.INFINITE_DURATION,
               PRIMARY_ATTACK_SLOW_EFFECT_AMPLIFIER,
                false,
                false
            )
        )
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.SLOW_FALLING,
                PotionEffect.INFINITE_DURATION,
                PRIMARY_ATTACK_SLOW_FALLING_AMPLIFIER,
                true,
               true
            )
        )
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.JUMP,
                PotionEffect.INFINITE_DURATION,
                PRIMARY_ATTACK_JUMP_AMPLIFIER,
                true,
                true,
            )
        )
        playersOnPrimaryAttack.add(player.uniqueId)
    }

    private fun deactivatePrimaryAttack(player: Player) {
        player.removePotionEffect(PotionEffectType.SLOW)
        player.removePotionEffect(PotionEffectType.SLOW_FALLING)
        player.removePotionEffect(PotionEffectType.JUMP)
        playersOnPrimaryAttack.remove(player.uniqueId)
    }

    private fun throwJailProjectile(player: Player) {
        player.launchProjectile(
            Snowball::class.java,
            player.location.direction.multiply(JAIL_VELOCITY_MULTIPLIER)
        ).let {
            it.customName(Component.text(FangsPublicArgs.JAIL_NAME))
            it.shooter = player
        }
    }

    private fun spawnJailAreaEffect(entity: Entity) {
        entity.world.spawn(entity.location, AreaEffectCloud::class.java).let { cloud ->
            cloud.radius = JAIL_EFFECT_AREA
            cloud.addCustomEffect(
                PotionEffect(
                    FangsPublicArgs.JAIL_EFFECT,
                    JAIL_DURATION,
                    1
                ),
                true
            )
            cloud.duration = FangsPublicArgs.JAIL_EFFECT_DURATION
            cloud.waitTime = JAIL_WAIT_TIME

            if (entity is Player)
                cloud.ownerUniqueId = entity.uniqueId
            else if (entity is Projectile)
                cloud.ownerUniqueId = (entity.shooter as? Player)?.uniqueId
        }
    }

    private fun spawnJailedArea(entity: Entity, ownerUUID: UUID) {
        entity.world.spawn(entity.location, AreaEffectCloud::class.java).let { cloud ->
            cloud.radius = FangsPublicArgs.JAILED_AREA.toFloat()
            cloud.duration = JAIL_DURATION
            cloud.waitTime = JAIL_WAIT_TIME
            cloud.setParticle(Particle.DUST_COLOR_TRANSITION, Particle.DustTransition(Color.WHITE, Color.AQUA, 1F))
            cloud.customName(Component.text(FangsPublicArgs.JAIL_NAME))
            cloud.ownerUniqueId = ownerUUID
        }
    }

    private fun dashToJail(player: Player) : Boolean {
        val entities = JAIL_DASH_MAX_DISTANCE_RADIOS.let { player.getNearbyEntities(it, it, it) }

        for (entity in entities) {
            if (
                entity is AreaEffectCloud &&
                entity.ownerUniqueId == player.uniqueId &&
                player.hasLineOfSight(
                    entity.location.let {
                        it.y += 1
                        it
                    }
                )
            ) {
                val quantityOfPlayersJailed = FangsPublicArgs.JAILED_AREA.let { area ->
                    entity.getNearbyEntities(area, area, area).filter {
                        it is Player && it.hasPotionEffect(FangsPublicArgs.JAIL_EFFECT) && it.uniqueId != player.uniqueId
                    }.size
                }
                player.health = (player.health + JAIL_DEFAULT_HEAL).coerceAtMost(20.0)
                val finalHealth = player.health + (quantityOfPlayersJailed * JAIL_HEAL_PER_JAILED)
                if (finalHealth > 20) {
                    player.health = 20.0
                    player.addPotionEffect(
                        PotionEffect(
                            PotionEffectType.ABSORPTION,
                            entity.duration - entity.ticksLived + BONUS_JAIL_HEAL_TIME,
                            ((finalHealth - 20) / JAIL_HEAL_PER_JAILED).roundToInt() - 1
                        )
                    )
                } else {
                    player.health = finalHealth
                }

                val vectorToAdd = player.location.toVector().subtract(entity.location.toVector()).let {
                    it.multiply(-JAIL_DASH_MULTIPLIER)
                    it.y = it.y.coerceAtLeast(0.0) + 0.5
                    it
                }
                player.velocity = player.velocity.add(vectorToAdd)

                MakeLineBetweenTwoLocationsUseCase(
                    location1 = player.location,
                    location2 = entity.location,
                    particle = Particle.DUST_COLOR_TRANSITION,
                    spacePerParticle = 0.5,
                    dustTransition = Particle.DustTransition(Color.RED, Color.WHITE, 3f)
                )

                return true
            }

        }

        return false
    }
}