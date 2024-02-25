package me.nikodemos612.classfight.ui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.util.HSVLike
import org.bukkit.Bukkit
import org.bukkit.Material
import java.util.*
import kotlin.collections.HashMap


object FighterlessInventoryHandler {

    fun getFighterSelectionInventory() = HashMap<Int, ItemSlotArgs>().apply {
        for (i in listOf(22)) {
            var skillName: String
            var skillCategory: String
            var material: Material
            val lore = mutableListOf <Component>()
            var displayName: Component
            when (i) {
                22 -> {
                    skillName = "shotgunner"
                    skillCategory = "Classe"
                    material = Material.STICK
                    displayName = Component.text("Shotgunner").color(TextColor.color(HSVLike.fromRGB(255,255,255)))
                    lore.add(Component.text("").color(TextColor.color(HSVLike.fromRGB(255,255,255))))
                }
                else -> {
                    continue
                }
            }
            lore.add(Component.text(skillCategory).color(TextColor.color(HSVLike.fromRGB(255,255,255))))
            lore.add(Component.text(""))

            put(i, ItemSlotArgs(
                    skillName = skillName,
                    skillCategory = skillCategory,
                    material = material,
                    displayName = displayName,
                    lore = lore
            ))
        }
    }

    fun switchClass(uuid: UUID, className: String, handlers: List<DefaultInventoryHandler>, characterSelection: CharacterSelection) {
        characterSelection.playerTeam[uuid] = className

        for (handler in handlers) {
            if (handler.canHandle(className)) {
                Bukkit.getServer().getPlayer(uuid)?.let {
                    characterSelection.buildInventory(it , getFighterSelectionInventory())
                }
            }
        }
    }
}