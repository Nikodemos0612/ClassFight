package me.nikodemos612.classfight.utill.player

import java.util.*
import kotlin.collections.HashMap

class Cooldown {
    private val listOfCooldowns = HashMap<UUID, Long>()

    fun hasCooldown(playerUUID: UUID) : Boolean {
        val cooldown = listOfCooldowns[playerUUID]
        if(cooldown != null && cooldown < System.currentTimeMillis() ) {
            listOfCooldowns.remove(playerUUID)
        }

        return listOfCooldowns[playerUUID] != null
    }

    fun addCooldownToPlayer(playerUUID: UUID, cooldown: Long) {
        listOfCooldowns[playerUUID] = System.currentTimeMillis() + cooldown
    }
}