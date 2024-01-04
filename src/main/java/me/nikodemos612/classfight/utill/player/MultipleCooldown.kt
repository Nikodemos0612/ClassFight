package me.nikodemos612.classfight.utill.player

import java.util.*

class MultipleCooldown(private val maxQuantity: Int) {

    private val listOfPlayersCooldowns = HashMap<UUID, Array<Long?>>()

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


    fun addCooldown(player: UUID, cooldown: Long) {
        val cooldownToAdd = System.currentTimeMillis() + cooldown

        listOfPlayersCooldowns[player]?.let { safePlayerCooldowns ->
            for (index in safePlayerCooldowns.indices) {
                if (safePlayerCooldowns[index] == null) {
                    safePlayerCooldowns[index] = cooldownToAdd
                    return
                }
            }
        } ?: run {
            listOfPlayersCooldowns[player] = arrayOfNulls(maxQuantity)
            listOfPlayersCooldowns[player]?.set(0, cooldownToAdd)
        }
    }

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
}