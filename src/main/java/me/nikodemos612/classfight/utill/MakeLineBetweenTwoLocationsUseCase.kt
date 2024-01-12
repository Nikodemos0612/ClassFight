package me.nikodemos612.classfight.utill

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Entity

object MakeLineBetweenTwoLocationsUseCase{
    operator fun invoke (
        location1: Location,
        location2: Location,
        particle: Particle,
        spacePerParticle: Double,
        dustTransition : Particle.DustTransition? = null,
        dustOptions : Particle.DustOptions? = null
    ) {
        val world = location1.world
        if (world == location2.world) {
            val distance = location1.distance(location2)
            var distanceCovered = 0.0

            val currentVector = location1.toVector()
            val stepVector = location2.toVector().subtract(currentVector).normalize().multiply(spacePerParticle)

            currentVector.add(stepVector)

            while (distanceCovered < distance) {
                world.spawnParticle(particle, currentVector.x, currentVector.y, currentVector.z, 1, dustTransition ?: dustOptions)
                currentVector.add(stepVector)
                distanceCovered += spacePerParticle
            }
        }
    }
}