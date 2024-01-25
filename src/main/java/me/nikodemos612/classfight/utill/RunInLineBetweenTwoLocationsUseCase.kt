package me.nikodemos612.classfight.utill

import org.bukkit.Location
import org.bukkit.util.Vector

object RunInLineBetweenTwoLocationsUseCase{
    operator fun invoke (
        location1: Location,
        location2: Location,
        stepSize: Double,
        stepFun: (Vector) -> Unit
    ) {
        val world = location1.world
        if (world == location2.world) {
            val distance = location1.distance(location2)
            var distanceCovered = 0.0

            val currentVector = location1.toVector()
            val stepVector = location2.toVector().subtract(currentVector).normalize().multiply(stepSize)

            currentVector.add(stepVector)

            while (distanceCovered < distance) {
                stepFun(currentVector)
                currentVector.add(stepVector)
                distanceCovered += stepSize
            }
        }
    }
}