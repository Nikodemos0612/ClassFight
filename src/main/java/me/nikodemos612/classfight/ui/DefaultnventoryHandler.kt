package me.nikodemos612.classfight.ui

import java.util.UUID

abstract class DefaultInventoryHandler {

    open val skillSelectionInventory = HashMap<Int, ItemSlotArgs>()

    protected abstract val fighterTeamName: String
    fun canHandle(teamName: String) : Boolean = teamName == fighterTeamName

    open fun switchSkill(uuid: UUID, skillCategory: String, skillName: String) {}
}
