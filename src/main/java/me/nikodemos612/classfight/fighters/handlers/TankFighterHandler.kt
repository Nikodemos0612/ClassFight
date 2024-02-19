package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.fighters.DefaultFighterHandler
import me.nikodemos612.classfight.utill.cooldown.Cooldown
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.Arrow
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Snowball
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

//base = 0.2
private const val PLAYER_WALKSPEED = 0.21F

private const val DAMAGE_TAKEN_MULTIPLIER = 0.8F

private const val RIFLE_PROJECTILE_NAME = "rifleShot"
private const val RIFLE_SHOT_COOLDOWN = 500L
private const val RIFLE_RELOAD_COOLDOWN = 7500L
private const val RIFLE_RELOAD_AMMO = 10
private const val RIFLE_PROJECTILE_SPEED = 5F
private const val RIFLE_PROJECTILE_DAMAGE = 1.0

private const val MOLOTOV_RELOAD_COUNT = 15

class TankFighterHandler(private val plugin: Plugin) : DefaultFighterHandler()  {

    private val rifleShotCooldown = Cooldown()
    private val rifleReloadCooldown = Cooldown()

    private var ammoCount = 0
    private var reloadTaskID = 0

    private var molotovReload = 0

    override val fighterTeamName = "tank"
    override val walkSpeed = PLAYER_WALKSPEED

    override fun resetInventory(player: Player) {
        player.inventory.clear()
        player.inventory.setItem(0, ItemStack(Material.STICK, RIFLE_RELOAD_AMMO))
        player.inventory.setItem(2, ItemStack(Material.SNOWBALL, MOLOTOV_RELOAD_COUNT))
        player.inventory.heldItemSlot = 0

        ammoCount = RIFLE_RELOAD_AMMO
        molotovReload = MOLOTOV_RELOAD_COUNT

        Bukkit.getServer().scheduler.cancelTask(reloadTaskID)

        player.flySpeed = 0.1F
    }

    override fun resetCooldowns(player: Player) {
        val playerUUID = player.uniqueId

        rifleShotCooldown.resetCooldown(playerUUID)
        rifleReloadCooldown.resetCooldown(playerUUID)

        player.resetCooldown()
    }

    override fun onItemHeldChange(event: PlayerItemHeldEvent) {
        val player = event.player

        when (event.newSlot) {
            0 -> {

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
            EntityType.ARROW -> {
                when (projectile.customName()) {
                    Component.text() -> {
                        
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
                                    shooter.inventory.removeItem(ItemStack(Material.SNOWBALL, 1))
                                    molotovReload--
                                    if (molotovReload == 0) {
                                        shooter.inventory.setItem(2, ItemStack(Material.FIRE_CHARGE, 1))
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
        event.damage *= DAMAGE_TAKEN_MULTIPLIER
    }

    /**
     * Responsible for shooting the pistol
     *
     * @param Player The player that is shooting the shotgun
     */
    private fun shootRifle(player: Player) {
        //player.playSound(player, Sound.ITEM_TRIDENT_HIT_GROUND, 15F, 1F)
        player.launchProjectile(Arrow::class.java, player.location.direction.multiply(RIFLE_PROJECTILE_SPEED)).let{
            it.shooter = player
            it.customName(Component.text(RIFLE_PROJECTILE_NAME))
            it.setGravity(false)
            it.isSilent = true
        }

        ammoCount--
        if (ammoCount == 0) {
            player.inventory.setItem(0, ItemStack(Material.BARRIER))

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
        player.inventory.setItem(0, ItemStack(Material.STICK, RIFLE_RELOAD_AMMO))
        ammoCount = RIFLE_RELOAD_AMMO
    }


    private fun shootMolotov(player: Player) {
        //player.playSound(player, Sound.ITEM_TRIDENT_HIT_GROUND, 15F, 1F)
        player.launchProjectile(Snowball::class.java, player.location.direction.multiply(RIFLE_PROJECTILE_SPEED)).let{
            it.shooter = player
            it.customName(Component.text(RIFLE_PROJECTILE_NAME))
            it.setGravity(true)
            it.isSilent = true
        }
        molotovReload = MOLOTOV_RELOAD_COUNT
    }
}