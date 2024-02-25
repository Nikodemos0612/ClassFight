package me.nikodemos612.classfight.ui

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scoreboard.Team
import java.util.*
import kotlin.collections.HashMap

class CharacterSelection {

    val playerTeam = HashMap<UUID, String>()

    val teams = HashMap<String, Team>()

    fun resetPlayer(player: Player) {
        for (team in Bukkit.getScoreboardManager().mainScoreboard.teams) {
            if (team.name == "") {
                team.addPlayer(player)
                buildInventory(player, FighterlessInventoryHandler.getFighterSelectionInventory(), false)
                playerTeam[player.uniqueId] = team.name
            }
        }
    }
    fun buildInventory(
            player: Player,
            inventory: HashMap<Int, ItemSlotArgs>,
            shouldShowConfirmReturnOptions: Boolean = true
    ) {
        player.inventory.setItem(9, ItemStack(Material.GRAY_STAINED_GLASS_PANE))
        val itemMeta = player.inventory.getItem(9)?.itemMeta
        for (i in 9..35) {
            if (inventory.keys.contains(i)) {
                val slotItem = inventory[i]

                slotItem?.material?.let {
                    player.inventory.setItem(i, ItemStack(it))
                }

                itemMeta.let {
                    it?.displayName(slotItem?.displayName)
                    it?.lore(slotItem?.lore)
                }
            } else {
                player.inventory.setItem(i, ItemStack(Material.GRAY_STAINED_GLASS_PANE))

                itemMeta.let {
                    it?.displayName(Component.text(""))
                    it?.lore(emptyList())
                }
            }
            player.inventory.getItem(i)?.itemMeta = itemMeta
        }
        /*
                player.inventory.setItem(39, ItemStack(Material.GRAY_STAINED_GLASS_PANE))
                itemMeta.let {
                    it?.displayName(Component.text(""))
                    it?.lore(emptyList())
                }
                player.inventory.getItem(39)?.itemMeta = itemMeta

                player.inventory.setItem(38, ItemStack(Material.GRAY_STAINED_GLASS_PANE))
                itemMeta.let {
                    it?.displayName(Component.text(""))
                    it?.lore(emptyList())
                }
                player.inventory.getItem(38)?.itemMeta = itemMeta
*/
        if (shouldShowConfirmReturnOptions) {
            player.inventory.setItem(37, ItemStack(Material.GREEN_STAINED_GLASS_PANE))
            itemMeta.let {
                it?.displayName(Component.text("CONFIRMAR"))
                it?.lore(emptyList())
            }
            player.inventory.getItem(37)?.itemMeta = itemMeta

            player.inventory.setItem(36, ItemStack(Material.RED_STAINED_GLASS_PANE))
            itemMeta.let {
                it?.displayName(Component.text("VOLTAR"))
                it?.lore(emptyList())
            }
            player.inventory.getItem(36)?.itemMeta = itemMeta
        } else {
            player.inventory.setItem(37, null)
            player.inventory.setItem(36, null)
        }

    }
}