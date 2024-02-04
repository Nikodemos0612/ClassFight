package me.nikodemos612.classfight.fighters

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import me.nikodemos612.classfight.fighters.handlers.GrapplerFighterHandler
import me.nikodemos612.classfight.fighters.handlers.HeavyHammerFighterHandler
import me.nikodemos612.classfight.fighters.handlers.PotionDealerFighterHandler
import me.nikodemos612.classfight.fighters.handlers.ShotgunnerFighterHandler
import me.nikodemos612.classfight.fighters.handlers.SniperFighterHandler
import org.bukkit.Bukkit
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.EvokerFangs
import org.bukkit.entity.Explosive
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.Plugin

/**
 * This class is used to handle all the fighters from the game.
 *
 * Want to add a fighter? Just create a new Class, make it inherit the DefaultFighterHandler and add it to the handlers
 * list on this class. Vouala! You created a new fighter! Just don't forget to implement it "canHandle" function
 * comparing their parameter with the name of your fighter, so this class can verify if it should run your class or not.
 *
 * @param plugin the plugin. If you want to print something in console, just add the plugin to the constructor of your
 * fighter, and use "plugin.logger.info()".
 * @author Nikodemos0612 (Lucas Coimbra).
 */
class FighterHandlerListeners(private val plugin: Plugin): Listener{

    //TODO: Alert!!! This causes a Memory Leak!
    /*
    * Why: Let's imagine you have infinite instances of Fighters;
    *     Basically, all the fighters would be instantiated at once at the start of the plugin, even tho we aren't
    * using any, and would never be caught by the garbage collector. Also, the way that this is implemented makes a
    * verification for all the fighters, event tho there isn't infinite players on the server on all of those infinite
    * fighters teams.
    *
    * Solution:
    *  * Create a companion object only for the verification (you don't need to instantiate the class only to
    * verify if a string is the same as another);
    *  * Make a list of companion objects
    *  * Make an empty list of the Handlers;
    *  * Instantiate a new handler on the empty list only when at least one player is on that team (that is the hard
    * part, there isn't any "ScoreBoardChangeEvent" or something like that)
    *  * Remove the handler of the list if there isn't any players on the team.
    *  * Make the rest of the code use this new list.
    *
    *    This way you can remove the instance of the handler if you are not using it, and you only instantiate it when
    * you actually need it. The list of companion objects is used to verify if the team has a class, and if it has,
    * instantiate it if the team has at least 1 player. It is also used to be an easy implementation of new classes,
    * such as now.
    *
    * (Note: I am not doing allatad 🤣)
    * Nikodemos
    */
    //TODO: Search about AOP
    /**
     * This is the list of fighter handlers.
     * Just add your fighter here if you created a new one!
     */
    private val handlers = listOf(
        SniperFighterHandler(plugin),
        PotionDealerFighterHandler(plugin),
        ShotgunnerFighterHandler(plugin),
        GrapplerFighterHandler(plugin),
        HeavyHammerFighterHandler(plugin)
    )

    /**
     * This function handles the PlayerItemHeldEvent, and passes it to the other handlers if they can handle it.
     */
    @EventHandler
    fun runItemHeldChangeHandler(event: PlayerItemHeldEvent) {
        getTeamName(from = event.player)?.let { safeTeamName ->
           for (handler in handlers) {
               if (handler.canHandle(safeTeamName))
                   handler.onItemHeldChange(event)
           }
        }
    }

    /**
     * This function handles the PlayerInteractEvent, and passes it to the other handlers if they can handle it.
     */
    @EventHandler
    fun runPlayerInteractionHandler(event: PlayerInteractEvent) {
        getTeamName(from = event.player)?.let{ safeTeamName ->
            for (handler in handlers) {
                if (handler.canHandle(safeTeamName))
                    handler.onPlayerInteraction(event)
            }
        }
    }

    /**
     * This function handles the ProjectileHitEvent, and passes it to the other handlers if they can handle it.
     */
    @EventHandler
    fun runProjectileHitHandler(event: ProjectileHitEvent) {
        val shooter = event.entity.shooter
        if (shooter is Player) {
            getTeamName(from = shooter)?.let { safeTeamName ->
                for (handler in handlers) {
                    if (handler.canHandle(safeTeamName))
                        handler.onProjectileHit(event)
                }
            }
        }
    }

    @EventHandler
    fun runPlayerHitByTeamEntityHandler(event: EntityDamageByEntityEvent) {
        if (event.entity is Player) {
            when (val damager = event.damager) {
                is Player -> damager
                is Projectile -> (damager.shooter as? Player)
                is AreaEffectCloud -> {
                    damager.ownerUniqueId?.let { shooterUUID ->
                        Bukkit.getPlayer(shooterUUID)
                    }
                }
                is EvokerFangs -> (damager.owner as? Player)
                is Explosive -> (damager.origin as? Player)
                else -> null
            }?.let { safePlayer ->
                getTeamName(from = safePlayer)?.let { safeTeamName ->
                    for (handler in handlers) {
                        if (handler.canHandle(safeTeamName))
                            handler.onPlayerHitByEntityFromThisTeam(event)
                    }
                }
            }
        }
    }

    @EventHandler
    fun runOnPlayerMoveHandler(event: PlayerMoveEvent) {
        getTeamName(from = event.player)?.let { safeTeamName ->
            for (handler in handlers) {
                if (handler.canHandle(safeTeamName))
                    handler.onPlayerMove(event)
            }
        }
    }

    /**
     * This function handles the PlayerRespawnEvent, resetting the player's cooldowns and inventory, just to be safe.
     */
    @EventHandler
    fun resetPlayerInventoryAndCooldownsOnRespawn(event: PlayerPostRespawnEvent) {
        val player = event.player
        getTeamName(from = player)?.let { safeTeamName ->
            for (handler in handlers) {
                if (handler.canHandle(safeTeamName)) {
                    handler.resetInventory(player = player)
                    handler.resetCooldowns(player = player)
                    player.walkSpeed = handler.walkSpeed
                }
            }
        }
    }

    // If you need to add another event to your fighter, just add it here.
    // And don't forget to also add to the event on the DefaultFighterHandler Interface.

    /**
     * Gets the name of the team from the Player
     * @param from the Player that could be on a team.
     * @return the name of the team of the player, if it has one.
     */
    private fun getTeamName(from: Player) =
        Bukkit.getScoreboardManager().mainScoreboard.getPlayerTeam(from)?.name
}