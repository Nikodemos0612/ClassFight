package me.nikodemos612.classfight.utill.player

import java.util.*

class MultipleCooldown(private val maxQuantity: Int) {

    private val listOfPlayersCooldowns = HashMap<UUID, Array<Long?>>()

    fun getCooldownQuantity(player: UUID): Int {
        listOfPlayersCooldowns[player]?.let {
            var quantity = 0

            for ((index, cooldown) in it.withIndex()) {
                if (cooldown != null && !it.isOutdated(index)) {
                    quantity++
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
            var quantityNull = 0

            for ((index, cooldown) in it.withIndex()) {
                if (cooldown == null || it.isOutdated(index)) {
                    result = false
                    quantityNull++
                }
            }

            if (quantityNull >= maxQuantity)
                listOfPlayersCooldowns.remove(player)

            return result
        } ?: return false
    }


    fun addCooldown(player: UUID, cooldown: Long) {
        val cooldownToAdd = System.currentTimeMillis() + cooldown

        listOfPlayersCooldowns[player]?.let {
            for ((index, cooldownOnList) in it.withIndex()) {
                if (cooldownOnList == null || it.isOutdated(index) ) {
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

    private fun Array<Long?>.isOutdated(index: Int) : Boolean {
        val isOutdated = this[index]?.let {
            it < System.currentTimeMillis()
        } ?: false

        if (isOutdated) this[index] = null

        return isOutdated
    }
}