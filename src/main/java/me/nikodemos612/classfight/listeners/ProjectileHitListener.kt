package me.nikodemos612.classfight.listeners

import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.util.Vector

class ProjectileHitListener : Listener{
    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val projectile = event.entity
        if (event.hitBlock != null && projectile is Snowball){

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
    }

    private fun mirrorVector(vectorToMirror: Vector, hitBlockFaceVector: Vector): Vector {
        return if(hitBlockFaceVector.x != 0.0) {
            vectorToMirror.multiply(Vector(-1, 1, 1))
        } else if (hitBlockFaceVector.y != 0.0) {
            vectorToMirror.multiply(Vector(1, -1, 1))
        } else {
            vectorToMirror.multiply(Vector(1, 1, -1))
        }
    }
}