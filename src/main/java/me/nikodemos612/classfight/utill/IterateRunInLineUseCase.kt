package me.nikodemos612.classfight.utill

import org.bukkit.Location
import org.bukkit.util.Vector

object IterateRunInLineUseCase {
    operator fun invoke (
        location: Location,
        direction: Vector,
        iterations: Int,
        stepFun: (location: Vector, iterator: Int) -> Unit
    ) {
        val currentVector = location.toVector()

        for (i in 0..<iterations) {
            stepFun(currentVector, i)
            currentVector.add(direction)
        }
    }
}