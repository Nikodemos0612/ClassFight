package me.nikodemos612.classfight.effects

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World

enum class PlayerEffect {
    POTION_BLINDED;

    fun spawnParticle(x: Double, y: Double, z: Double, world: World) {
        when (this) {
            POTION_BLINDED -> world.spawnParticle(
                Particle.GLOW_SQUID_INK,
                x,
                y,
                z,
                1
            )
        }
    }
}