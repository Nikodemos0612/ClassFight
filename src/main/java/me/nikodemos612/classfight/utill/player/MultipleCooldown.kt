package me.nikodemos612.classfight.utill.player

import java.util.*

class MultipleCooldown(private val maxQuantity: Int) {

    private val listOfPlayersCooldowns = HashMap<UUID, Array<Long?>>()

    fun getCooldownQuantity(player: UUID): Int {
        listOfPlayersCooldowns[player]?.let {
            var quantity = 0

            for (index in it.indices) {
                val cooldown = it[index]
                if (cooldown != null) {
                    if (cooldown < System.currentTimeMillis()) {
                        it[index] = null
                    } else quantity++
                }
            }

            if (quantity == 0)
                listOfPlayersCooldowns.remove(player)

            return quantity
        } ?: return 0
    }

    fun isAllInCooldown(player: UUID): Boolean {
        listOfPlayersCooldowns[player]?.let {
            var result = true
            var isAllEmpty = true

            for (index in it.indices) {
                val cooldown = it[index]

                if (cooldown == null || cooldown < System.currentTimeMillis()) {
                    it[index] = null
                    result = false
                } else isAllEmpty = false
            }

            if (isAllEmpty)
                listOfPlayersCooldowns.remove(player)

            return result
        } ?: return false
    }


    fun addCooldown(player: UUID, cooldown: Long) {
        val cooldownToAdd = System.currentTimeMillis() + cooldown

        listOfPlayersCooldowns[player]?.let {
            for ((index, cooldownOnList) in it.withIndex()) {
                if (cooldownOnList == null || cooldownOnList < System.currentTimeMillis()) {
                    it[index] = cooldownToAdd
                    return
                }
            }
        } ?: {
            val arrayToAdd = arrayOfNulls<Long>(maxQuantity)
            arrayToAdd[0] = cooldownToAdd
            listOfPlayersCooldowns[player] = arrayToAdd
        }
    }
}