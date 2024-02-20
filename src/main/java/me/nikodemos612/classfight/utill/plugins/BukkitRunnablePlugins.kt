package me.nikodemos612.classfight.utill.plugins

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

inline fun runAsync(plugin: Plugin, crossinline function: () -> Unit) {
    Bukkit.getScheduler().runTaskAsynchronously(
        plugin,
        Runnable {
            function()
        }
    )
}

inline fun runAsyncLater(plugin: Plugin, ticksDelay: Long, crossinline function: () -> Unit): Int =
    Bukkit.getScheduler().runTaskLaterAsynchronously(
        plugin,
        Runnable {
            function()
        },
        ticksDelay
    ).taskId


inline fun runLater(plugin: Plugin, ticksDelay: Long, crossinline function: () -> Unit): Int =
    Bukkit.getScheduler().runTaskLater(
        plugin,
        Runnable {
            function()
        },
        ticksDelay
    ).taskId
