package me.nikodemos612.classfight.utill.player

import java.util.*
import kotlin.collections.HashMap

/**
 * This class is used to handle the cooldown for a given player, defined by their UUID.
 *
 * @see MultipleCooldown if you want to create multiple cooldowns for the same ability/other of a player
 * @author Nikodemos0612 (Lucas Coimbra).
 */
class Cooldown {

    /**
     * The list of all cooldowns from all the players.
     *
     * It is a HashMap, that has the key as the player UUID, and returns a Long that represents when the Cooldown will
     * be outdated, based on the current time.
     */
    private val listOfCooldowns = HashMap<UUID, Long>()

    /**
     * Given a UUID of a Player, returns false if the player don't have an outdated cooldown.
     *
     * @param playerUUID the UUID of the Player.
     * @return a Boolean that represents if the player has or not a cooldown.
     *
     */
    fun hasCooldown(playerUUID: UUID) : Boolean {
        val cooldown = listOfCooldowns[playerUUID]
        if(cooldown != null && cooldown < System.currentTimeMillis() ) {
            listOfCooldowns.remove(playerUUID)
        }

        return listOfCooldowns[playerUUID] != null
    }

    /**
     * Given a UUID of a Player and a cooldown, adds the cooldown to the Player.
     *
     * @param playerUUID the UUID of the Player.
     * @param cooldown the cooldown to add to the given Player.
     */
    fun addCooldownToPlayer(playerUUID: UUID, cooldown: Long) {
        listOfCooldowns[playerUUID] = System.currentTimeMillis() + cooldown
    }

    /**
     * Given a UUID of a Player, removes the cooldown from the Player.
     *
     * @param from the UUID of the player that will have their cooldown removed, if existent.
     */
    fun resetCooldown(from: UUID) {
        listOfCooldowns.remove(from)
    }

}