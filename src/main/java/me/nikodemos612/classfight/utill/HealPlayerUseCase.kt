package me.nikodemos612.classfight.utill

import org.bukkit.entity.Player

/**
 * This class is used to handle player healing
 *
 * @author Gumend3s (Gustavo Mendes).
 */
object HealPlayerUseCase {
    operator fun invoke(player: Player, healAmount: Double): Boolean {
        if (player.isDead || player.health >= 20.0 || healAmount <= 0.0)
            return false

        if (player.health + healAmount > 20.0) {
                player.health = 20.0
        } else player.health += healAmount

        return true
    }
}