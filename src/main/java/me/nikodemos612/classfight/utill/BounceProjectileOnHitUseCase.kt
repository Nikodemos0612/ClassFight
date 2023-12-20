package me.nikodemos612.classfight.utill

import org.bukkit.entity.Snowball
import org.bukkit.entity.ThrownPotion
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
                    }
                }
            }

            is ThrownPotion -> {
                event.hitBlockFace?.let { safeHitBlockFace ->
                    projectile.remove()

                    val mirrorVector = mirrorVector(
                        vectorToMirror = projectile.velocity,
                        hitBlockFaceVector = safeHitBlockFace.direction
                    ).multiply(0.5)

                    projectile.world.spawn(projectile.location, ThrownPotion::class.java).let {
                        it.shooter = projectile.shooter
                        it.velocity = mirrorVector
                        it.potionMeta.color = projectile.potionMeta.color
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