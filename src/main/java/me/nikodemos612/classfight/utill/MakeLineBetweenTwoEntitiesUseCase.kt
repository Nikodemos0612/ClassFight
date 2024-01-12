package me.nikodemos612.classfight.utill

import org.bukkit.Particle
import org.bukkit.entity.Entity

object MakeLineBetweenTwoEntitiesUseCase{
    operator fun invoke (
        entity1: Entity,
        entity2: Entity,
        particle: Particle,
        spacePerParticle: Double,
        dustTransition : Particle.DustTransition? = null,
        dustOptions : Particle.DustOptions? = null
    ) {
        val world = entity1.world
        if (world == entity2.world) {
            val distance = entity1.location.distance(entity2.location)
            var distanceCovered = 0.0

            val currentVector = entity1.location.toVector()
            val stepVector = entity2.location.toVector().subtract(currentVector).normalize().multiply(spacePerParticle)

            currentVector.add(stepVector)

            while (distanceCovered < distance) {
                world.spawnParticle(particle, currentVector.x, currentVector.y, currentVector.z, 1, dustTransition ?: dustOptions)
                currentVector.add(stepVector)
                distanceCovered += spacePerParticle
            }
        }
    }
}