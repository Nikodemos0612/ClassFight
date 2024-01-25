package me.nikodemos612.classfight.utill.cooldown

import java.util.*


/**
 * This class is used to handle multiple cooldowns for a given player, defined by their UUID.
 *
 * @param maxQuantity the max quantity of cooldowns of the given ability/other per Player.
 *
 * @see Cooldown if you want to use just one cooldown per Player for a certain ability or other.
 * @author Nikodemos0612 (Lucas Coimbra).
 */
class MultipleCooldown(private val maxQuantity: Int) {

    /**
     * The list of all cooldowns from all the players.
     *
     * It is a HashMap, that has the key as the player UUID, and returns a Long that represents when the Cooldown will
     * be outdated, based on the current time.
     */
    private val listOfPlayersCooldowns = HashMap<UUID, Array<Long?>>()

    /**
     * Given a UUID of a Player, returns false if the player has at least 1 empty or outdated cooldown.
     *
     * @param player the UUID of the Player.
     * @return a Boolean that represents if the player has at least 1 empty or outdated cooldown.
     */
    fun isAllInCooldown(player: UUID): Boolean {
        listOfPlayersCooldowns[player]?.let { safePlayerCooldowns ->
            var withoutCooldownCount = 0
            for ((index, cooldown) in safePlayerCooldowns.withIndex()) {
                if (cooldown == null)
                    withoutCooldownCount++
                else if (cooldown < System.currentTimeMillis()) {
                    safePlayerCooldowns[index] = null
                    withoutCooldownCount++
                }
            }

            if (withoutCooldownCount >= maxQuantity)
                listOfPlayersCooldowns.remove(player)

            return withoutCooldownCount == 0
        }
        return false
    }

    /**
     * Given a UUID of a Player and a cooldown, adds the cooldown to the Player, if there is an empty or outdated
     * cooldown.
     *
     * @param player the UUID of the Player.
     * @param cooldown the cooldown to add to the given Player.
     * @return a boolean that represents if the operation what successful or not.
     */
    fun addCooldown(player: UUID, cooldown: Long): Boolean {
        val cooldownToAdd = System.currentTimeMillis() + cooldown

        listOfPlayersCooldowns[player]?.let { safePlayerCooldowns ->
            for ((index, cooldownToVerify) in safePlayerCooldowns.withIndex()) {
                if (cooldownToVerify == null || cooldownToVerify < System.currentTimeMillis()) {
                    safePlayerCooldowns[index] = cooldownToAdd
                    return true
                }
            }
            return false
        } ?: run {
            listOfPlayersCooldowns[player] = arrayOfNulls(maxQuantity)
            listOfPlayersCooldowns[player]?.set(0, cooldownToAdd)
            return true
        }
    }

    /**
     * Returns the quantity of cooldowns that the given player has, des-considering outdated ones.
     *
     * @param player the UUID of the player
     * @return an Integer that represents the quantity of cooldowns that the given player has.
     */
    fun getCooldownQuantity(player: UUID): Int {
        listOfPlayersCooldowns[player]?.let { safePlayerCooldowns ->
            var quantity = 0
            for ((index, cooldown) in safePlayerCooldowns.withIndex()) {
                if (cooldown != null) {
                    if (cooldown < System.currentTimeMillis())
                        safePlayerCooldowns[index] = null
                    else quantity++
                }
            }

            if (quantity == 0)
                listOfPlayersCooldowns.remove(player)

            return quantity
        }

        return 0
    }

    /**
     * Returns the list of cooldowns that the given player has, des-considering outdated ones.
     *
     * @param player the UUID of the player
     * @return a List of time stamps, measured in milliseconds, that represents when the cooldown will be outdated based
     * on the current time.
     * @see System.currentTimeMillis
     */
    fun getCooldowns(player: UUID): List<Long>? {
        return listOfPlayersCooldowns[player]?.let { safePlayersCooldowns ->
            val listOfCooldown = mutableListOf<Long>()

            for ((index, cooldown) in safePlayersCooldowns.withIndex()) {
                if (cooldown != null && cooldown > System.currentTimeMillis()) {
                   listOfCooldown.add(cooldown)
                } else safePlayersCooldowns[index] = null
            }

            if (listOfCooldown.isEmpty())
                listOfPlayersCooldowns.remove(player)

            listOfCooldown
        }
    }

    /**
     * Removes all cooldowns from a given Player, if the given player has any.
     *
     * @param from the player that will have all their cooldowns removed
     */
    fun resetCooldowns(from: UUID) {
        listOfPlayersCooldowns.remove(from)
    }
}