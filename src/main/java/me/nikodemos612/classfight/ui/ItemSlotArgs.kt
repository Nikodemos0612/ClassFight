package me.nikodemos612.classfight.ui

import net.kyori.adventure.text.Component
import org.bukkit.Material


data class ItemSlotArgs (
        var skillName: String,
        var skillCategory: String,
        var material: Material,
        var displayName: Component,
        var lore: MutableList<Component>
)