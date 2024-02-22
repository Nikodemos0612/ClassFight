package me.nikodemos612.classfight.ui

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import kotlin.collections.HashMap

class CharacterSelection(plugin: Plugin) {
    fun buildInventory(player: Player, inventory: HashMap<Int, ItemSlotArgs>) {
        player.inventory.setItem(9, ItemStack(Material.GRAY_STAINED_GLASS_PANE))
        val itemMeta = player.inventory.getItem(9)?.itemMeta
        for (i in 9..35) {
            val slotItem = inventory[i]

            slotItem?.material?.let {
                player.inventory.setItem(i, ItemStack(it))
            }

            itemMeta.let {
                it?.displayName(slotItem?.displayName)
                if (slotItem?.material != Material.GRAY_STAINED_GLASS_PANE) {
                    it?.lore(slotItem?.lore)
                } else {
                    it?.lore(emptyList())
                }
            }
            player.inventory.getItem(i)?.itemMeta = itemMeta
        }
    }
}