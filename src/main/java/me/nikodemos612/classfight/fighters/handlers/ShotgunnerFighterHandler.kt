package me.nikodemos612.classfight.fighters.handlers

import me.nikodemos612.classfight.fighters.DefaultFighterHandler
import me.nikodemos612.classfight.utill.HealPlayerUseCase
import me.nikodemos612.classfight.utill.cooldown.Cooldown
import me.nikodemos612.classfight.utill.plugins.runLater
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.entity.Arrow
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

private val REG_SHOTGUN_ITEM = Material.STICK
private const val REG_SHOTGUN_PROJECTILE_NAME = "regShotgunShot"
private const val REG_SHOTGUN_AMMO_COUNT = 1
private const val REG_SHOTGUN_SHOT_COOLDOWN = 2500L
private const val REG_SHOTGUN_RELOAD_COOLDOWN = 5500L
private const val REG_SHOTGUN_PROJECTILE_DURATION = 3L
private const val REG_SHOTGUN_PROJECTILE_SPEED = 10F
private const val REG_SHOTGUN_PROJECTILE_DAMAGE = 1.0
private const val REG_SHOTGUN_PROJECTILE_AMOUNT = 14
private const val REG_SHOTGUN_PROJECTILE_SPREAD = 20F

private val MINI_SHOTGUN_ITEM = Material.BLAZE_ROD
private const val MINI_SHOTGUN_PROJECTILE_NAME = "altShotgunShot"
private const val MINI_SHOTGUN_PROJECTILE_DAMAGE = 1.0
private const val MINI_SHOTGUN_BASE_AMMO = 2
private const val MINI_SHOTGUN_ADD_AMMO = 2
private const val MINI_SHOTGUN_BASE_COOLDOWN = 1000L
private const val MINI_SHOTGUN_PROJECTILE_DURATION = 4L
private const val MINI_SHOTGUN_PROJECTILE_SPEED = 10F
private const val MINI_SHOTGUN_PROJECTILE_AMOUNT = 8
private const val MINI_SHOTGUN_PROJECTILE_SPREAD = 10F

private val HORIZONTAL_DASH_ITEM = Material.ENDER_PEARL
private const val HORIZONTAL_DASH_Y = 0.5
private const val HORIZONTAL_DASH_STRENGTH = 1.0
private const val HORIZONTAL_DASH_COOLDOWN = 7000L

private val VERTICAL_DASH_ITEM = Material.ENDER_EYE
private const val VERTICAL_DASH_Y = 10.0
private const val VERTICAL_DASH_STRENGTH  = 0.8
private const val VERTICAL_DASH_COOLDOWN = 4500L

private val REG_PISTOL_ITEM = Material.TRIPWIRE_HOOK
private const val REG_PISTOL_PROJECTILE_NAME = "pistolShot"
private const val REG_PISTOL_SHOT_COOLDOWN = 8000L
private const val REG_PISTOL_PROJECTILE_SPEED = 4F
private const val REG_PISTOL_PROJECTILE_DAMAGE = 8.0
private const val REG_PISTOL_HEAL_EFFECT_STRENGTH = 12.0
private const val REG_PISTOL_PULL_STRENGTH = 0.4
private const val REG_PISTOL_ADD_COOLDOWN_RATIO = 2.0F


/**
 * This class handles the ShotgunnerFighter and all it's events.
 *
 * @author Gumend3s (Gustavo Mendes)
 * @see Cooldown
 * @see DefaultFighterHandler
 *
 * The shotgunner is a class based on mostly close range combat, and its main gimmick is the kit switch.
 *
 * The player will have a primary weapon that can be used as soon as they spawn, alongside with a dash ability.
 *
 * But using their secondary weapon the player is able to switch to an alternative kit, that has a limited use primary
 * weapon, that get extra charges when using the secondary again, and a different dash ability.
 * (the secondary weapon remains the same at all times)
 *
 * As soon as the charges from the alt primary are used the players goes back to the regular primary and dash.
 */
class ShotgunnerFighterHandler(private val plugin: Plugin) : DefaultFighterHandler() {

    private val primaryCooldown = Cooldown()
    private val secondaryCooldown = Cooldown()
    private val dashCooldown = Cooldown()

    private var primaryWeapon = ""
    private var primaryAltWeapon = ""
    private var secondaryWeapon = ""
    private var dash = ""
    private var dashAlt = ""

    private var primaryWeaponAmmoCount = 0
    private var primaryReloadID = 0
    
    private var altKit = false

    override val fighterTeamName = "shotgunner"

    override fun resetInventory(player: Player) {
        primaryWeapon = "regShotgun"
        primaryAltWeapon = "regShotgunAlt"
        secondaryWeapon = "regPistol"
        dash = "horizontalDash"
        dashAlt = "verticalDash"
        
        player.inventory.clear()
        when (primaryWeapon) {
            "regShotgun" -> {
                player.inventory.setItem(0, ItemStack(REG_SHOTGUN_ITEM, REG_SHOTGUN_AMMO_COUNT))
                primaryWeaponAmmoCount = REG_SHOTGUN_AMMO_COUNT
            }
        }
        when (secondaryWeapon) {
            "regPistol" -> {
                player.inventory.setItem(1, ItemStack(REG_PISTOL_ITEM))
            }
        }
        when (dash) {
            "horizontalDash" -> {
                player.inventory.setItem(2, ItemStack(HORIZONTAL_DASH_ITEM))
            }

            "verticalDash" -> {
                player.inventory.setItem(2, ItemStack(VERTICAL_DASH_ITEM))
            }
        }
        player.inventory.heldItemSlot = 0

        player.allowFlight = true
        player.flySpeed = 0F

        if (player.gameMode == GameMode.CREATIVE) {
            player.flySpeed = 1F
        }
    }

    override fun resetCooldowns(player: Player) {
        val playerUUID = player.uniqueId

        primaryCooldown.resetCooldown(playerUUID)
        secondaryCooldown.resetCooldown(playerUUID)
        dashCooldown.resetCooldown(playerUUID)
        player.resetCooldown()
    }

    override fun onItemHeldChange(event: PlayerItemHeldEvent) {
        val player = event.player

        when (event.newSlot) {
            0 -> {}
        }
        player.inventory.heldItemSlot = 0
    }

    override fun onPlayerInteraction(event: PlayerInteractEvent) {
        val player = event.player

        when {
            event.action.isLeftClick -> handleLeftClick(player)

            event.action.isRightClick -> handleRightClick(player)
        }

    }

    override fun onPlayerHitByEntityFromThisTeam(event: EntityDamageByEntityEvent) {
        when (event.cause) {
            EntityDamageEvent.DamageCause.ENTITY_ATTACK -> (event.damager as? Player)?.let {
                handleLeftClick(it)
                event.damage = 0.0
            }
            else -> {
                (event.damager as? Projectile)?.let { projectile ->
                    when (projectile.customName()) {
                        Component.text(REG_SHOTGUN_PROJECTILE_NAME) -> {
                            (event.entity as? Player)?.let {
                                it.playSound(it, Sound.BLOCK_LAVA_POP, 20F, 1F)
                            }
                            event.damage = REG_SHOTGUN_PROJECTILE_DAMAGE
                        }
                        
                        Component.text(MINI_SHOTGUN_PROJECTILE_NAME) -> {
                            (event.entity as? Player)?.let {
                                it.playSound(it, Sound.BLOCK_LAVA_POP, 20F, 1F)
                            }
                            event.damage = MINI_SHOTGUN_PROJECTILE_DAMAGE
                        }
                        
                        Component.text(REG_PISTOL_PROJECTILE_NAME) -> {
                            event.damage = REG_PISTOL_PROJECTILE_DAMAGE
                            (projectile.shooter as? Player)?.let {  shooter ->
                                shooter.playSound(shooter, Sound.ITEM_ARMOR_EQUIP_LEATHER, 18F, 1F)
                                (event.entity as? Player)?.let {
                                    it.playSound(it, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 10F, 1F)
                                }

                                HealPlayerUseCase(shooter, REG_PISTOL_HEAL_EFFECT_STRENGTH)

                                val velocity = shooter.location.toVector().subtract(event.entity.location.toVector())
                                event.entity.velocity = velocity.setY(velocity.y.coerceAtLeast(0.25))
                                        .multiply(REG_PISTOL_PULL_STRENGTH)
                                
                                if (!altKit) {
                                    switchKits(shooter)
                                } else {
                                    addPrimaryAltAmmo(shooter)
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun handleLeftClick(player: Player) {
        if (!primaryCooldown.hasCooldown(player.uniqueId)) {
            shootPrimary(player)
        }
    }

    private fun handleRightClick(player: Player) {
        if (!secondaryCooldown.hasCooldown(player.uniqueId)) {
            shootSecondary(player)
        }
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player

        if (event.player.isOnGround) {
            player.removePotionEffect(PotionEffectType.JUMP)
        }

        if (event.player.isFlying) {
            if (!dashCooldown.hasCooldown(player.uniqueId)) {
                if (!altKit) {
                    activateDash(player, dash)
                } else {
                    activateDash(player, dashAlt)
                }
            }
            event.player.isFlying = false
        }
    }

    override fun onPlayerDamage(event: EntityDamageEvent) {
        (event.entity as? Player)?.removePotionEffect(PotionEffectType.JUMP)
    }

    /**
     * Shoots the primary weapon selected, being either the weapon from its regular kit or from the alt one
     *
     * @param player The player that is shooting the weapon
     */
    private fun shootPrimary(player: Player) {
        if (!altKit) {
            when (primaryWeapon) {
                "regShotgun" -> {
                    player.playSound(player, Sound.ITEM_CROSSBOW_SHOOT, 1F, 1F)


                    for(i in 1..REG_SHOTGUN_PROJECTILE_AMOUNT) {
                        player.world.spawnArrow(
                                player.eyeLocation,
                                player.eyeLocation.direction,
                                REG_SHOTGUN_PROJECTILE_SPEED,
                                REG_SHOTGUN_PROJECTILE_SPREAD
                        ).let{
                            it.shooter = player
                            it.customName(Component.text(REG_SHOTGUN_PROJECTILE_NAME))
                            it.setGravity(false)
                            it.isSilent = true
                        }
                    }

                    runLater(plugin, REG_SHOTGUN_PROJECTILE_DURATION) {
                        deleteProjectiles(player, REG_SHOTGUN_PROJECTILE_NAME)
                    }

                    player.inventory.removeItem(ItemStack(REG_SHOTGUN_ITEM, 1))
                    primaryWeaponAmmoCount--
                    if (primaryWeaponAmmoCount != 0) {
                        primaryCooldown.addCooldownToPlayer(player.uniqueId, REG_SHOTGUN_SHOT_COOLDOWN)
                        player.setCooldown(player.inventory.getItem(0)?.type ?: Material.BEDROCK, (REG_SHOTGUN_SHOT_COOLDOWN/50).toInt())
                    } else {
                        player.inventory.setItem(0, ItemStack(Material.BARRIER))
                        primaryCooldown.addCooldownToPlayer(player.uniqueId, REG_SHOTGUN_RELOAD_COOLDOWN)
                        player.setCooldown(player.inventory.getItem(0)?.type ?: Material.BEDROCK, (REG_SHOTGUN_RELOAD_COOLDOWN/50).toInt())

                        primaryReloadID = runLater(plugin, REG_SHOTGUN_RELOAD_COOLDOWN/50) {
                            reloadPrimary(player)
                        }
                    }

                }
            }
        } else {
            when (primaryAltWeapon) {
                "regShotgunAlt" -> {
                    player.playSound(player, Sound.ITEM_SPYGLASS_USE, 1F, 1F)


                    for(i in 1..MINI_SHOTGUN_PROJECTILE_AMOUNT) {
                        player.world.spawnArrow(
                                player.eyeLocation,
                                player.eyeLocation.direction,
                                MINI_SHOTGUN_PROJECTILE_SPEED,
                                MINI_SHOTGUN_PROJECTILE_SPREAD
                        ).let{
                            it.shooter = player
                            it.customName(Component.text(MINI_SHOTGUN_PROJECTILE_NAME))
                            it.setGravity(false)
                            it.isSilent = true
                        }
                    }

                    runLater(plugin, MINI_SHOTGUN_PROJECTILE_DURATION) {
                        deleteProjectiles(player, MINI_SHOTGUN_PROJECTILE_NAME)
                    }

                    player.inventory.removeItem(ItemStack(MINI_SHOTGUN_ITEM, 1))
                    primaryWeaponAmmoCount--

                    if (primaryWeaponAmmoCount != 0) {
                        primaryCooldown.addCooldownToPlayer(player.uniqueId, MINI_SHOTGUN_BASE_COOLDOWN)
                        player.setCooldown(player.inventory.getItem(0)?.type ?: Material.BEDROCK, (MINI_SHOTGUN_BASE_COOLDOWN/50).toInt())
                    } else {
                        switchKits(player)
                    }
                }
            }
        }
    }

    /**
     * Reloads the ammo of the primary weapon from the regular kit
     *
     * @param player The player that receives its weapon respective ammo
     */
    private fun reloadPrimary(player: Player) {
        when (primaryWeapon) {
            "regShotgun" -> {
                player.inventory.setItem(0, ItemStack(REG_SHOTGUN_ITEM))
                primaryWeaponAmmoCount = REG_SHOTGUN_AMMO_COUNT
            }
        }
    }

    /**
     * Shoots the secondary weapon selected
     *
     * @param player The player that is shooting the weapon
     */
    private fun shootSecondary(player: Player) {
        when (secondaryWeapon) {
            "regPistol" -> {
                player.playSound(player, Sound.ITEM_TRIDENT_HIT_GROUND, 1F, 1F)
                
                
                player.launchProjectile(Arrow::class.java, player.location.direction.multiply(REG_PISTOL_PROJECTILE_SPEED)).let{
                    it.shooter = player
                    it.customName(Component.text(REG_PISTOL_PROJECTILE_NAME))
                    it.setGravity(false)
                    it.isSilent = true
                }

                secondaryCooldown.addCooldownToPlayer(player.uniqueId, REG_PISTOL_SHOT_COOLDOWN)
                player.setCooldown(player.inventory.getItem(1)?.type ?: Material.BEDROCK, (REG_PISTOL_SHOT_COOLDOWN/50).toInt())
            }
        }
    }

    /**
     * Adds ammo to the primary weapon of the alt kit
     *
     * @param player The player that is receiving the ammo
     */
    private fun addPrimaryAltAmmo(player: Player) {
        when (primaryAltWeapon) {
            "regShotgunAlt" -> {
                player.inventory.addItem(ItemStack(MINI_SHOTGUN_ITEM, MINI_SHOTGUN_ADD_AMMO))
                primaryWeaponAmmoCount += MINI_SHOTGUN_ADD_AMMO
            }
        }
        
        player.setCooldown(player.inventory.getItem(0)?.type ?: Material.BEDROCK, (MINI_SHOTGUN_BASE_COOLDOWN/50).toInt())
        primaryCooldown.addCooldownToPlayer(player.uniqueId, MINI_SHOTGUN_BASE_COOLDOWN)
    }

    /**
     * Activates the selected dash
     *
     * @param player The player doing the dash
     * @param dashName The name of the dash to be activated
     */
    private fun activateDash(player: Player, dashName: String) {
        when (dashName) {
            "horizontalDash" -> {
                player.playSound(player, Sound.BLOCK_PISTON_EXTEND, 1F, 1F)

                player.velocity = player.eyeLocation.direction.setY(0).normalize().setY(HORIZONTAL_DASH_Y).normalize().multiply(HORIZONTAL_DASH_STRENGTH)
                dashCooldown.addCooldownToPlayer(player.uniqueId, HORIZONTAL_DASH_COOLDOWN)
                player.setCooldown(player.inventory.getItem(2)?.type ?: Material.BEDROCK, (HORIZONTAL_DASH_COOLDOWN/50).toInt())
                player.addPotionEffect(
                        PotionEffect(
                                PotionEffectType.JUMP,
                                PotionEffect.INFINITE_DURATION,
                                1,
                                false,
                                false,
                                true,
                        ),
                )
            }

            "verticalDash" -> {
                player.playSound(player, Sound.BLOCK_PISTON_CONTRACT, 1F, 1F)

                player.velocity = player.eyeLocation.direction.setY(0).normalize().setY(VERTICAL_DASH_Y).normalize().multiply(VERTICAL_DASH_STRENGTH)
                dashCooldown.addCooldownToPlayer(player.uniqueId, VERTICAL_DASH_COOLDOWN)
                player.setCooldown(player.inventory.getItem(2)?.type ?: Material.BEDROCK, (VERTICAL_DASH_COOLDOWN/50).toInt())
                player.addPotionEffect(
                        PotionEffect(
                                PotionEffectType.JUMP,
                                PotionEffect.INFINITE_DURATION,
                                1,
                                false,
                                false,
                                true,
                        ),
                )
            }
        }
    }

    /**
     * Switches from one kit to the other
     *
     * @param player The player that has its kit switched
     */
    private fun switchKits(player: Player) {
        if (altKit) {
            when (primaryWeapon) {
                "regShotgun" -> {
                    player.inventory.setItem(0, ItemStack(REG_SHOTGUN_ITEM))
                    primaryWeaponAmmoCount = REG_SHOTGUN_AMMO_COUNT
                    primaryCooldown.addCooldownToPlayer(player.uniqueId, REG_SHOTGUN_SHOT_COOLDOWN)
                    player.setCooldown(player.inventory.getItem(0)?.type ?: Material.BEDROCK, (REG_SHOTGUN_SHOT_COOLDOWN/50).toInt())
                }
            }

            when (dash) {
                "horizontalDash" -> {
                    player.inventory.setItem(2, ItemStack(HORIZONTAL_DASH_ITEM))
                    player.setCooldown(player.inventory.getItem(2)?.type ?: Material.BEDROCK, 0)
                    dashCooldown.resetCooldown(player.uniqueId)
                }

                "verticalDash" -> {
                    player.inventory.setItem(2, ItemStack(VERTICAL_DASH_ITEM))
                    player.setCooldown(player.inventory.getItem(2)?.type ?: Material.BEDROCK, 0)
                    dashCooldown.resetCooldown(player.uniqueId)
                }
            }
        } else {
            Bukkit.getServer().scheduler.cancelTask(primaryReloadID)
            var addCooldown = 0

            when (primaryAltWeapon) {
                "regShotgunAlt" -> {
                    player.inventory.setItem(0, ItemStack(MINI_SHOTGUN_ITEM, MINI_SHOTGUN_BASE_AMMO))
                    primaryWeaponAmmoCount = MINI_SHOTGUN_BASE_AMMO
                    addCooldown = (primaryCooldown.returnCooldown(player.uniqueId) / REG_PISTOL_ADD_COOLDOWN_RATIO).toInt()
                }
            }

            val newCooldown = ((MINI_SHOTGUN_BASE_COOLDOWN + addCooldown)/50).toInt()

            player.setCooldown(player.inventory.getItem(0)?.type ?: Material.BEDROCK, newCooldown)
            primaryCooldown.addCooldownToPlayer(player.uniqueId, newCooldown.toLong())

            when (dashAlt) {
                "horizontalDash" -> {
                    player.inventory.setItem(2, ItemStack(HORIZONTAL_DASH_ITEM))
                    player.setCooldown(player.inventory.getItem(2)?.type ?: Material.BEDROCK, 0)
                    dashCooldown.resetCooldown(player.uniqueId)
                }

                "verticalDash" -> {
                    player.inventory.setItem(2, ItemStack(VERTICAL_DASH_ITEM))
                    player.setCooldown(player.inventory.getItem(2)?.type ?: Material.BEDROCK, 0)
                    dashCooldown.resetCooldown(player.uniqueId)
                }
            }
        }
        altKit = !altKit
    }

    /**
     * Deletes all projectiles that meet the criteria passed
     *
     * @param owner The player that shot the projectiles
     * @param projName The customName of the projectiles
     */
    private fun  deleteProjectiles(owner: Player, projName: String) {
        for (entity in owner.world.entities) {
            (entity as? Projectile)?.let { projectile ->
                if (projectile.shooter == owner && projectile.customName() == Component.text(projName)) {
                    projectile.remove()
                }
            }
        }
    }
}