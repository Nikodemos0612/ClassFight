package me.nikodemos612.classfight.utill

import me.nikodemos612.classfight.utill.plugins.safeLet
import org.bukkit.entity.ThrowableProjectile
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.util.Vector

/**
 * This class is used to let a projectile bounce on walls.
 *
 * @author Nikodemos0612 (Lucas Coimbra).
 */
object BounceProjectileOnHitUseCase {

    /**
     * Given a projectileHitEvent and if the projectile has hit a wall, this function will make the projectile bounce
     * the wall.
     *
     * @param event the ProjectileHitEvent, that was called when the projectile have hit the wall.
     * @param friction the loss or gain on velocity that the projectile will have on bouncing on the wall. Has the
     * default value of 0.5.
     */
    operator fun invoke(event: ProjectileHitEvent, friction: Double = 0.5) {
        when (val projectile = event.entity) {
            is ThrowableProjectile-> {
                safeLet(projectile.type.entityClass, event.hitBlockFace) { safeProjectileType, safeHitBlockFace ->
                    val mirrorVector = mirrorVector(
                        vectorToMirror = projectile.velocity,
                        hitBlockFaceVector = safeHitBlockFace.direction
                    ).multiply(friction)

                    (projectile.world.spawn(projectile.location, safeProjectileType) as? ThrowableProjectile)?.let {
                        it.velocity = mirrorVector
                        it.shooter = projectile.shooter
                        it.customName(projectile.customName())
                    }
                }
            }
        }
    }

    /**
     * Mirrors the given vector to the new vector that represents the bounced projectile velocity vector
     *
     * @param vectorToMirror the velocity of the object that has hit the wall
     * @param hitBlockFaceVector the vector that represents what face the object has hit
     */
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