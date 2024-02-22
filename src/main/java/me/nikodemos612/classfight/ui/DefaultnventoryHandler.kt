package me.nikodemos612.classfight.ui

import net.kyori.adventure.text.Component
import org.bukkit.Material
import java.util.UUID

abstract class DefaulInventoryHandler {

    open val skillSelectionInventory = HashMap<Int, ItemSlotArgs>()

    protected abstract val fighterTeamName: String
    fun canHandle(teamName: String) : Boolean = teamName == fighterTeamName

    open fun switchSkill(uuid: UUID, skillCategory: String, skillName: String) {}
}

data class ItemSlotArgs (
        var skillName: String,
        var skillCategory: String,
        var material: Material,
        var displayName: Component,
        var lore: MutableList<Component>
)