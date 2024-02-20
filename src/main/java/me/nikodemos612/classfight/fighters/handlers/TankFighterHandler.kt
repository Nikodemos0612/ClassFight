package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.fighters.DefaultFighterHandler
import me.nikodemos612.classfight.utill.BounceProjectileOnHitUseCase
import me.nikodemos612.classfight.utill.HealPlayerUseCase
import me.nikodemos612.classfight.utill.cooldown.Cooldown
import net.kyori.adventure.text.Component
import org.bukkit.*
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
import java.util.UUID
import java.util.Vector

//base = 0.2
private const val PLAYER_WALKSPEED = 0.21F

private const val DAMAGE_TAKEN_MULTIPLIER = 0.8

private const val RIFLE_PROJECTILE_NAME = "rifleShot"
private const val RIFLE_SHOT_COOLDOWN = 300L
private const val RIFLE_RELOAD_COOLDOWN = 5500L
private const val RIFLE_RELOAD_COOLDOWN_MIN = 2000L
private const val RIFLE_RELOAD_AMMO = 10
private const val RIFLE_PROJECTILE_SPEED = 5F
private const val RIFLE_PROJECTILE_DAMAGE = 2.0

private const val MOLOTOV_PROJECTILE_NAME = "molotovShot"
private const val MOLOTOV_PROJECTILE_BOUNCE_FRICTION = 0.6
private const val MOLOTOV_PROJECTILE_SPEED = 1.5F
private const val MOLOTOV_RELOAD_COUNT = 7
private const val MOLOTOV_AREA_RADIUS = 4.0
private const val MOLOTOV_AREA_HEIGHT = 2.0
private const val MOLOTOV_PARTICLE_AMOUNT = 300
private const val MOLOTOV_DAMAGE = 1.5
private const val MOLOTOV_WAIT_TIME = 20L
private const val MOLOTOV_DAMAGE_TICKS = 10
private const val MOLOTOV_DAMAGE_TICKS_DELAY = 6L

private const val HEAL_PROJECTILE_NAME = "healShot"
private const val HEAL_PROJECTILE_BOUNCE_FRICTION = 0.6
private const val HEAL_PROJECTILE_SPEED = 1.5F
private const val HEAL_COOLDOWN = 35000L
private const val HEAL_AREA_RADIUS = 4.0
private const val HEAL_AREA_HEIGHT = 1.0
private const val HEAL_PARTICLE_AMOUNT = 50
private const val HEAL_PARTICLE_TICK_RATIO = 2.5
private const val HEAL_SOUND_TICK_RATIO = 10
private const val HEAL_AMMOUNT = 0.5
private const val HEAL_WAIT_TIME = 0L
private const val HEAL_TICKS = 50
private const val HEAL_TICKS_DELAY = 2L
//base = 0.2
private const val HEAL_PLAYER_WALKSPEED = 0.23F
private const val HEAL_DAMAGE_TAKEN_MULTIPLIER = 0.5

class TankFighterHandler(private val plugin: Plugin) : DefaultFighterHandler()  {

    private var damageTakenMultiplier = 1.0

    private val rifleShotCooldown = Cooldown()
    private val rifleReloadCooldown = Cooldown()
    private val healCooldown = Cooldown()

    private var ammoCount = 0
    private var reloadTaskID = 0

    private var molotovReload = 0

    private var playersInsideHealAOE = mutableListOf<UUID>()

    override val fighterTeamName = "tank"
    override val walkSpeed = PLAYER_WALKSPEED

    override fun resetInventory(player: Player) {
        player.inventory.clear()
        player.inventory.setItem(0, ItemStack(Material.STICK, RIFLE_RELOAD_AMMO))
        player.inventory.setItem(1, ItemStack(Material.MANGROVE_PROPAGULE))
        player.inventory.setItem(2, ItemStack(Material.SNOWBALL, MOLOTOV_RELOAD_COUNT))
        player.inventory.setItem(3, ItemStack(Material.BARRIER))
        player.inventory.heldItemSlot = 0

        damageTakenMultiplier = DAMAGE_TAKEN_MULTIPLIER

        ammoCount = RIFLE_RELOAD_AMMO
        molotovReload = MOLOTOV_RELOAD_COUNT

        Bukkit.getServer().scheduler.cancelTask(reloadTaskID)

        player.flySpeed = 0.1F
    }

    override fun resetCooldowns(player: Player) {
        val playerUUID = player.uniqueId

        rifleShotCooldown.resetCooldown(playerUUID)
        rifleReloadCooldown.resetCooldown(playerUUID)
        healCooldown.resetCooldown(playerUUID)

        player.resetCooldown()
    }

    override fun onItemHeldChange(event: PlayerItemHeldEvent) {
        val player = event.player

        when (event.newSlot) {
            0 -> {

            }

            1 -> {
                if (!healCooldown.hasCooldown(player.uniqueId)) {
                    shootHeal(player)
                }
                player.inventory.heldItemSlot = 0
            }

            3 -> {
                if (!rifleReloadCooldown.hasCooldown((player.uniqueId)) && ammoCount < RIFLE_RELOAD_AMMO) {
                    player.playSound(player, Sound.ITEM_CROSSBOW_LOADING_START, 1F, 1F)
                    val cooldownPerAmmo = (RIFLE_RELOAD_COOLDOWN - RIFLE_RELOAD_COOLDOWN_MIN) / RIFLE_RELOAD_AMMO
                    val ammoSpent = RIFLE_RELOAD_AMMO - ammoCount
                    val newReloadCooldown = RIFLE_RELOAD_COOLDOWN_MIN + (cooldownPerAmmo * (ammoSpent))

                    ammoCount = 0

                    player.inventory.setItem(0, ItemStack(Material.BARRIER))
                    player.inventory.setItem(3, ItemStack(Material.BARRIER))

                    rifleReloadCooldown.addCooldownToPlayer(player.uniqueId, newReloadCooldown)
                    player.setCooldown(Material.BARRIER, (newReloadCooldown/50).toInt())

                    Bukkit.getServer().scheduler.runTaskLater(plugin, reloadRifle(player), newReloadCooldown/50).let {
                        reloadTaskID = it.taskId
                    }
                }
                player.inventory.heldItemSlot = 0
            }

            else -> {
                player.inventory.heldItemSlot = 0
            }
        }
    }

    override fun onPlayerInteraction(event: PlayerInteractEvent) {
        val player = event.player

        when {
            event.action.isLeftClick -> handleLeftClick(player)

            event.action.isRightClick -> handleRightClick(player)
        }
    }

    override fun onProjectileHit(event: ProjectileHitEvent) {

        val projectile = event.entity as? Projectile
        when (projectile?.type) {
            EntityType.SNOWBALL -> {
                when (projectile.customName()) {
                    Component.text(MOLOTOV_PROJECTILE_NAME) -> {
                        if (event.hitEntity != null || event.hitBlockFace == BlockFace.UP) {
                            spawnMolotovAOE(projectile)
                        } else {
                            BounceProjectileOnHitUseCase(event, MOLOTOV_PROJECTILE_BOUNCE_FRICTION)
                        }
                    }

                    Component.text(HEAL_PROJECTILE_NAME) -> {
                        if (event.hitEntity != null || event.hitBlockFace == BlockFace.UP) {
                            spawnHealAOE(projectile)
                        } else {
                            BounceProjectileOnHitUseCase(event, HEAL_PROJECTILE_BOUNCE_FRICTION)
                        }
                    }
                    else -> {}
                }
            }
            else -> {}
        }
        event.entity.remove()
    }

    override fun onPlayerHitByEntityFromThisTeam(event: EntityDamageByEntityEvent) {
        when (event.cause) {
            EntityDamageEvent.DamageCause.ENTITY_ATTACK -> (event.damager as? Player)?.let {
                handleLeftClick(it)
                event.damage = 0.0
            }

            else -> {
                when (val damager = event.damager) {
                    is Projectile -> {
                        when (damager.customName()) {
                            Component.text(RIFLE_PROJECTILE_NAME) -> {
                                event.damage = RIFLE_PROJECTILE_DAMAGE
                                (damager.shooter as? Player)?.let {shooter ->
                                    shooter.playSound(shooter, Sound.ENTITY_ARROW_HIT_PLAYER, 1F, 1F)
                                    shooter.playSound(shooter, Sound.BLOCK_BAMBOO_HIT, 1F, 1F)
                                    shooter.inventory.removeItem(ItemStack(Material.SNOWBALL, 1))
                                    if (molotovReload != 0) {
                                        molotovReload--
                                        if (molotovReload == 0) {
                                            shooter.inventory.setItem(2, ItemStack(Material.FIRE_CHARGE, 1))
                                        }
                                    }
                                }
                            }

                            else -> {}
                        }
                    }

                    else -> {

                    }
                }
            }
        }
    }

    private fun handleLeftClick(player: Player) {
        if (!rifleShotCooldown.hasCooldown(player.uniqueId) && !rifleReloadCooldown.hasCooldown(player.uniqueId)) {
            when (player.inventory.getItem(0)?.type) {
                Material.STICK -> {
                    shootRifle(player)
                }
                else -> {}
            }
        }
    }

    private fun handleRightClick(player: Player) {
        if (molotovReload == 0) {
            shootMolotov(player)
        }
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
    }

    override fun onPlayerDamage(event: EntityDamageEvent) {
        event.damage *= damageTakenMultiplier
    }

    /**
     * Responsible for shooting the pistol
     *
     * @param player The player that is shooting the shotgun
     */
    private fun shootRifle(player: Player) {
        player.playSound(player, Sound.BLOCK_DEEPSLATE_BREAK, SoundCategory.PLAYERS, 1F, 1F, 0)
        player.launchProjectile(Arrow::class.java, player.location.direction.multiply(RIFLE_PROJECTILE_SPEED)).let{
            it.shooter = player
            it.customName(Component.text(RIFLE_PROJECTILE_NAME))
            it.setGravity(false)
            it.isSilent = true
        }

        ammoCount--
        if (ammoCount == RIFLE_RELOAD_AMMO - 1) {
            player.inventory.setItem(3, ItemStack(Material.DRIED_KELP))
        }
        if (ammoCount == 0) {
            player.playSound(player, Sound.ITEM_CROSSBOW_LOADING_START, 1F, 1F)
            player.inventory.setItem(0, ItemStack(Material.BARRIER))
            player.inventory.setItem(3, ItemStack(Material.BARRIER))

            rifleReloadCooldown.addCooldownToPlayer(player.uniqueId, RIFLE_RELOAD_COOLDOWN)
            player.setCooldown(Material.BARRIER, (RIFLE_RELOAD_COOLDOWN/50).toInt())

            Bukkit.getServer().scheduler.runTaskLater(plugin, reloadRifle(player), RIFLE_RELOAD_COOLDOWN/50).let {
                reloadTaskID = it.taskId
            }
        } else {
            player.inventory.removeItem(ItemStack(Material.STICK, 1))

            rifleShotCooldown.addCooldownToPlayer(player.uniqueId, RIFLE_SHOT_COOLDOWN)
            player.setCooldown(Material.STICK, (RIFLE_SHOT_COOLDOWN/50).toInt())
        }

    }

    private fun reloadRifle(player: Player) = Runnable {
        player.playSound(player, Sound.ITEM_CROSSBOW_LOADING_END, 1F, 1F)
        player.inventory.setItem(0, ItemStack(Material.STICK, RIFLE_RELOAD_AMMO))
        ammoCount = RIFLE_RELOAD_AMMO
    }


    private fun shootMolotov(player: Player) {
        player.playSound(player, Sound.ENTITY_BLAZE_SHOOT, 1F, 1F)
        player.launchProjectile(Snowball::class.java, player.location.direction.multiply(MOLOTOV_PROJECTILE_SPEED)).let{
            it.shooter = player
            it.customName(Component.text(MOLOTOV_PROJECTILE_SPEED))
            it.setGravity(true)
            it.isSilent = true
            it.customName(Component.text(MOLOTOV_PROJECTILE_NAME))
        }
        
        molotovReload = MOLOTOV_RELOAD_COUNT
        player.inventory.setItem(2, ItemStack(Material.SNOWBALL, MOLOTOV_RELOAD_COUNT))
    }

    private fun spawnMolotovAOE(projectile: Projectile) {
        val location = projectile.location
        projectile.world.spawn(location, AreaEffectCloud::class.java).let { cloud ->
            cloud.duration = (MOLOTOV_DAMAGE_TICKS * MOLOTOV_DAMAGE_TICKS_DELAY + MOLOTOV_WAIT_TIME)
                    .toInt()
            cloud.waitTime = 0
            cloud.setParticle(Particle.DUST_COLOR_TRANSITION, Particle.DustTransition(Color.AQUA, Color.FUCHSIA, 1f))
            cloud.radius = MOLOTOV_AREA_RADIUS.toFloat()
            cloud.ownerUniqueId = projectile.ownerUniqueId

            (projectile.shooter as? Player)?.let { owner ->
                owner.playSound(owner, Sound.ENTITY_BLAZE_HURT, 1F, 1F)
                Bukkit.getServer().scheduler.runTaskLater(
                        plugin,
                        damageMolotovAOE(cloud, owner, 0),
                        MOLOTOV_WAIT_TIME
                )
            }
        }
    }

    private fun damageMolotovAOE(areaEffectCloud: AreaEffectCloud, owner: Player, repetitions: Int) = Runnable {
        val aoeCenter = Location(areaEffectCloud.world, areaEffectCloud.x, areaEffectCloud.y + (MOLOTOV_AREA_HEIGHT/2), areaEffectCloud.z)
        areaEffectCloud.world.spawnParticle(
                Particle.FALLING_OBSIDIAN_TEAR,
                aoeCenter,
                MOLOTOV_PARTICLE_AMOUNT,
                MOLOTOV_AREA_RADIUS / 2.5,
                MOLOTOV_AREA_HEIGHT / 2,
                MOLOTOV_AREA_RADIUS / 2.5,
                0.0,
        )
        for (player in areaEffectCloud.world.getNearbyPlayers(aoeCenter, MOLOTOV_AREA_RADIUS, MOLOTOV_AREA_HEIGHT, MOLOTOV_AREA_RADIUS)) {
            if (player != owner) {
                player.damage(MOLOTOV_DAMAGE)
                player.playSound(player, Sound.ENTITY_BLAZE_HURT, 1F, 1F)
                owner.playSound(owner, Sound.BLOCK_FIRE_EXTINGUISH, 1F, 1F)
            }
        }
        repeatDamageMolotovAOE(areaEffectCloud,owner, repetitions + 1)
    }

    private fun repeatDamageMolotovAOE(areaEffectCloud: AreaEffectCloud, owner: Player, repetitions: Int) {
        if (repetitions < MOLOTOV_DAMAGE_TICKS) {
            Bukkit.getServer().scheduler.runTaskLater(
                    plugin,
                    damageMolotovAOE(areaEffectCloud, owner, repetitions),
                    MOLOTOV_DAMAGE_TICKS_DELAY
            )
        }
    }

    private fun shootHeal(player: Player) {
        player.playSound(player, Sound.ENTITY_SHULKER_SHOOT, 1F, 1F)
        player.launchProjectile(Snowball::class.java, player.location.direction.multiply(HEAL_PROJECTILE_SPEED)).let{
            it.shooter = player
            it.customName(Component.text(HEAL_PROJECTILE_SPEED))
            it.setGravity(true)
            it.isSilent = true
            it.customName(Component.text(HEAL_PROJECTILE_NAME))
        }

        healCooldown.addCooldownToPlayer(player.uniqueId, HEAL_COOLDOWN)
        player.setCooldown(Material.MANGROVE_PROPAGULE, (HEAL_COOLDOWN/50).toInt())
    }

    private fun spawnHealAOE(projectile: Projectile) {
        val location = projectile.location
        projectile.world.spawn(location, AreaEffectCloud::class.java).let { cloud ->
            cloud.duration = (HEAL_TICKS * HEAL_TICKS_DELAY + HEAL_WAIT_TIME)
                    .toInt()
            cloud.waitTime = 0
            cloud.setParticle(Particle.DUST_COLOR_TRANSITION, Particle.DustTransition(Color.LIME, Color.BLACK, 1f))
            cloud.radius = HEAL_AREA_RADIUS.toFloat()
            cloud.ownerUniqueId = projectile.ownerUniqueId

            (projectile.shooter as? Player)?.let { owner ->
                Bukkit.getServer().scheduler.runTaskLater(
                        plugin,
                        healHealAOE(cloud,owner, 0),
                        HEAL_WAIT_TIME
                )
            }
        }
    }

    private fun healHealAOE(areaEffectCloud: AreaEffectCloud, owner:Player, repetitions: Int) = Runnable {
        val newPlayersInsideHealAOE = mutableListOf<UUID>()
        val aoeCenter = Location(areaEffectCloud.world, areaEffectCloud.x, areaEffectCloud.y + (HEAL_AREA_HEIGHT/2), areaEffectCloud.z)
        if (repetitions % HEAL_PARTICLE_TICK_RATIO == 0.0) {
            areaEffectCloud.world.spawnParticle(
                    Particle.VILLAGER_HAPPY,
                    aoeCenter,
                    HEAL_PARTICLE_AMOUNT,
                    HEAL_AREA_RADIUS / 2.5,
                    HEAL_AREA_HEIGHT / 2,
                    HEAL_AREA_RADIUS / 2.5,
                    0.0,
            )
        }
        if (repetitions % HEAL_SOUND_TICK_RATIO == 0) {
            owner.playSound(owner, Sound.BLOCK_NOTE_BLOCK_BELL, 1F, 1F)
        }
        for (player in areaEffectCloud.world.getNearbyPlayers(aoeCenter, HEAL_AREA_RADIUS, HEAL_AREA_HEIGHT, HEAL_AREA_RADIUS)) {
            newPlayersInsideHealAOE.add(player.uniqueId)
            if (player == owner) {
                if (!playersInsideHealAOE.contains(player.uniqueId)) {
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1F)
                    damageTakenMultiplier = HEAL_DAMAGE_TAKEN_MULTIPLIER
                    owner.walkSpeed = HEAL_PLAYER_WALKSPEED
                }
                HealPlayerUseCase(player, HEAL_AMMOUNT)
            } else {
                if (!playersInsideHealAOE.contains(player.uniqueId)) {
                    player.playSound(player, Sound.BLOCK_HONEY_BLOCK_HIT, 0.8F, 1F)
                    owner.playSound(owner, Sound.BLOCK_NOTE_BLOCK_HARP, 1F, 1F)
                }
                player.addPotionEffect(PotionEffect(
                        PotionEffectType.SLOW,
                        PotionEffect.INFINITE_DURATION,
                        2,
                        false,
                        false)
                )
            }
        }

        for (playerUUID in playersInsideHealAOE) {
            if (!newPlayersInsideHealAOE.contains(playerUUID)) {
                Bukkit.getPlayer(playerUUID)?.let { player ->
                    if (player == owner) {
                        damageTakenMultiplier = DAMAGE_TAKEN_MULTIPLIER
                        owner.walkSpeed = PLAYER_WALKSPEED
                        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_HAT, 1F, 1F)
                    } else {
                        player.removePotionEffect(PotionEffectType.SLOW)
                        player.playSound(player, Sound.BLOCK_CHAIN_BREAK, 0.8F, 1F)
                        owner.playSound(owner, Sound.BLOCK_NOTE_BLOCK_COW_BELL, 0.8F, 1F)
                    }
                }
            }
        }
        playersInsideHealAOE = newPlayersInsideHealAOE

        repeatHealHealAOE(areaEffectCloud, owner, repetitions + 1)
    }

    private fun repeatHealHealAOE(areaEffectCloud: AreaEffectCloud, owner: Player, repetitions: Int) {
        if (repetitions < HEAL_TICKS) {
            Bukkit.getServer().scheduler.runTaskLater(
                    plugin,
                    healHealAOE(areaEffectCloud, owner, repetitions),
                    HEAL_TICKS_DELAY
            )
        } else {
            damageTakenMultiplier = DAMAGE_TAKEN_MULTIPLIER
            owner.walkSpeed = PLAYER_WALKSPEED
            owner.playSound(owner, Sound.BLOCK_CHAIN_BREAK, 1F, 1F)

            for (playerUUID in playersInsideHealAOE) {
                Bukkit.getPlayer(playerUUID)?.let { player ->
                    if (player == owner) {
                        owner.playSound(owner, Sound.BLOCK_NOTE_BLOCK_HAT, 1F, 1F)
                    } else {
                        player.removePotionEffect(PotionEffectType.SLOW)
                        player.playSound(player, Sound.BLOCK_CHAIN_BREAK, 0.8F, 1F)
                        owner.playSound(owner, Sound.BLOCK_NOTE_BLOCK_COW_BELL, 0.8F, 1F)
                    }
                }
            }
            playersInsideHealAOE.clear()
        }
    }
}