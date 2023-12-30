package me.nikodemos612.classfight.utill

import org.bukkit.entity.Snowball
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.util.Vector

object BounceProjectileOnHitUseCase {
    operator fun invoke(event: ProjectileHitEvent) {
        when (val projectile = event.entity) {
            is Snowball -> {
                event.hitBlockFace?.let { safeHitBlockFace ->

                    val mirrorVector = mirrorVector(
                        vectorToMirror = projectile.velocity,
                        hitBlockFaceVector = safeHitBlockFace.direction
                    ).multiply(0.5)

                    projectile.world.spawn(projectile.location, Snowball::class.java).let {
                        it.shooter = projectile.shooter
                        it.velocity = mirrorVector
                        it.item = projectile.item
                    }
                }
            }
        }
    }

    private fun mirrorVector(vectorToMirror: Vector, hitBlockFaceVector: Vector): Vector {
        return if (hitBlockFaceVector.x != 0.0) {
            vectorToMirror.multiply(Vector(-1, 1, 1))
        } else if (hitBlockFaceVector.y != 0.0) {
            vectorToMirror.multiply(Vector(1, -1, 1))
        } else {
            vectorToMirror.multiply(Vector(1, 1, -1))
        }
    }
}