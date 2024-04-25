package me.nikodemos612.classfight.effects

import me.nikodemos612.classfight.utill.IterateRunInLineUseCase
import me.nikodemos612.classfight.utill.plugins.cancelTask
import me.nikodemos612.classfight.utill.plugins.repeatRun
import me.nikodemos612.classfight.utill.plugins.runAsync
import me.nikodemos612.classfight.utill.plugins.runLater
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.*
import org.bukkit.util.Vector
import kotlin.collections.HashMap

private typealias PlayerUUID = UUID

private const val INITIAL_TICKS_DELAY = 10L
private const val TICKS_DELAY = 2L

class DefaultPlayerEffectsHandler(val plugin: Plugin) {
    private data class PlayerEffectArgs(
        val playerEffect: PlayerEffect,
        val taskId: Int
    )

    private val playersEffects = HashMap<PlayerUUID, List<PlayerEffectArgs>>()

    private var playersEffectsTaskId: Int? = null

    fun addEffectOrReset(
        playerUUID: UUID,
        effect: PlayerEffect,
        effectDuration: Long
    ) {

        playersEffects[playerUUID]?.toMutableList()?.let { playerEffectArgsList ->
            for (effectArgs in playerEffectArgsList) {
                if (effectArgs.playerEffect == effect) {
                    cancelTask(taskId = effectArgs.taskId)
                    playerEffectArgsList.remove(effectArgs)
                }
            }

            val taskId = createRemovalEffectTaskById(
                playerUUID = playerUUID,
                effectDuration = effectDuration,
            )

            playerEffectArgsList.add(
                PlayerEffectArgs(
                    playerEffect = effect,
                    taskId = taskId
                )
            )

            return
        }


        val taskId = createRemovalEffectTaskById(
            playerUUID = playerUUID,
            effectDuration = effectDuration,
        )

        playersEffects[playerUUID] = listOf(
            PlayerEffectArgs(
                playerEffect = effect,
                taskId = taskId,
            )
        )
    }

    fun addEffect(
       playerUUID: UUID,
       effect: PlayerEffect,
       effectDuration: Long
    ) {
        playersEffects[playerUUID]?.toMutableList()?.let { playerEffectArgsList ->
            val taskId = createRemovalEffectTaskById(
                playerUUID = playerUUID,
                effectDuration = effectDuration,
            )

            playerEffectArgsList.add(
                PlayerEffectArgs(
                    playerEffect = effect,
                    taskId = taskId
                )
            )

            return
        }


        val taskId = createRemovalEffectTaskById(
            playerUUID = playerUUID,
            effectDuration = effectDuration,
        )

        playersEffects[playerUUID] = listOf(
            PlayerEffectArgs(
                playerEffect = effect,
                taskId = taskId,
            )
        )
    }

    private fun createRemovalEffectTaskById(
        playerUUID: UUID,
        effectDuration: Long
    ): Int {
        var taskId: Int? = null

        taskId = runLater(
            plugin = plugin,
            ticksDelay = effectDuration
        ) {
            playersEffects[playerUUID]?.toMutableList()?.let { effectList ->
                effectList.removeIf { it.taskId == taskId}
                playersEffects[playerUUID] = effectList
            }

            if (playersEffects[playerUUID]?.isEmpty() == true)
                playersEffects.remove(playerUUID)
        }

        return taskId
    }

    private fun spawnEffectsOnPlayer(player: Player) = playersEffects[player.uniqueId]?.let { playerEffectArgs ->
        IterateRunInLineUseCase(
            location = player.eyeLocation.add(Vector(0,1,0)),
            direction = Vector(0, 1, 0),
            iterations = playerEffectArgs.size
        ) { location, iterator ->
            runAsync(plugin,) {
                playerEffectArgs[iterator].playerEffect.spawnParticle(
                    x = location.x,
                    y = location.y,
                    z = location.z,
                    world = player.world
                )
            }
        }
    }

    fun createEffectsTask() {
        removeEffectsTask()
        val players = Bukkit.getOnlinePlayers()

        playersEffectsTaskId = repeatRun(
            plugin = plugin,
            initialTicksDelay = INITIAL_TICKS_DELAY,
            iterationsTickDelay = TICKS_DELAY,
        ) {
            for (player in players) {
                if (player.isOnline && !player.isDead) {
                    spawnEffectsOnPlayer(player)
                }
            }
        }
    }

    fun removeEffectsTask() {
        playersEffectsTaskId?.let { cancelTask(it) }
        playersEffectsTaskId = null
    }

    fun clearPlayerEffects(playerUUID: UUID) {
        playersEffects[playerUUID]?.let { effects ->
            for (effect in effects) {
                cancelTask(effect.taskId)
            }
            playersEffects.remove(playerUUID)
        }
    }

    fun clearPlayerEffect(playerUUID: UUID, effect: PlayerEffect) {
        playersEffects[playerUUID]?.toMutableList()?.let { effects ->
            for (effectArgs in effects) {
                if (effectArgs.playerEffect == effect) {
                    cancelTask(effectArgs.taskId)
                    effects.remove(effectArgs)
                }
            }
            playersEffects[playerUUID] = effects
        }
    }

    fun removeFirstPlayerEffect(playerUUID: UUID, effect: PlayerEffect) {
        playersEffects[playerUUID]?.toMutableList()?.let { effects ->
            for (effectArgs in effects) {
                if (effectArgs.playerEffect == effect) {
                    cancelTask(effectArgs.taskId)
                    effects.remove(effectArgs)
                    playersEffects[playerUUID] = effects
                    return
                }
            }
        }
    }

    fun hasPlayerEffect(playerUUID: UUID, effect: PlayerEffect): Boolean =
        playersEffects[playerUUID]?.any { it.playerEffect == effect } == true

    fun getEffectQuantity(playerUUID: UUID, effect: PlayerEffect): Int =
        playersEffects[playerUUID]?.let {
            var count = 0
            for (effectArgs in it) {
                if (effectArgs.playerEffect == effect)
                    count++
            }
            count
        } ?: 0

}