package me.nikodemos612.classfight.ui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.util.HSVLike
import org.bukkit.Material
import java.util.UUID

class ShotgunnerInventoryHandler: DefaultInventoryHandler() {

    override val fighterTeamName = "shotgunner"

    private val primaryWeapon = HashMap<UUID, String>()
    private var primaryAltWeapon = HashMap<UUID, String>()
    private var secondaryWeapon = HashMap<UUID, String>()
    private var dash = HashMap<UUID, String>()
    private var dashAlt = HashMap<UUID, String>()

    override val skillSelectionInventory = HashMap<Int, ItemSlotArgs>().apply {
        for (i in listOf(9, 11, 13, 15, 17, 24, 26)) {
            var skillName: String
            var skillCategory: String
            var material: Material
            val lore = mutableListOf <Component>()
            var displayName: Component
            when (i) {
                9 -> {
                    skillName = "regShotgun"
                    skillCategory = "Arma Primária"
                    material = Material.STICK
                    displayName = Component.text("Shotgun").color(TextColor.color(HSVLike.fromRGB(255,255,255)))
                    lore.add(Component.text("").color(TextColor.color(HSVLike.fromRGB(255,255,255))))
                }
                11 -> {
                    skillName = "regShotgunAlt"
                    skillCategory = "Arma Alternativa"
                    material = Material.BLAZE_ROD
                    displayName = Component.text("Mini Shotgun").color(TextColor.color(HSVLike.fromRGB(255,255,255)))
                    lore.add(Component.text("").color(TextColor.color(HSVLike.fromRGB(255,255,255))))
                }
                13 -> {
                    skillName = "regPistol"
                    skillCategory = "Arma Secundária"
                    material = Material.TRIPWIRE_HOOK
                    displayName = Component.text("Pistola").color(TextColor.color(HSVLike.fromRGB(255,255,255)))
                    lore.add(Component.text("").color(TextColor.color(HSVLike.fromRGB(255,255,255))))
                }
                15 -> {
                    skillName = "horizontalDash"
                    skillCategory = "Dash Primário"
                    material = Material.ENDER_PEARL
                    displayName = Component.text("Dash Horizontal").color(TextColor.color(HSVLike.fromRGB(255,255,255)))
                    lore.add(Component.text("").color(TextColor.color(HSVLike.fromRGB(255,255,255))))
                }
                24 -> {
                    skillName = "VerticalDash"
                    skillCategory = "Dash Primário"
                    material = Material.ENDER_EYE
                    displayName = Component.text("Dash Vertical").color(TextColor.color(HSVLike.fromRGB(255,255,255)))
                    lore.add(Component.text("").color(TextColor.color(HSVLike.fromRGB(255,255,255))))
                }
                17 -> {
                    skillName = "horizontalDash"
                    skillCategory = "Dash Alternativo"
                    material = Material.ENDER_PEARL
                    displayName = Component.text("Dash Horizontal").color(TextColor.color(HSVLike.fromRGB(255,255,255)))
                    lore.add(Component.text("").color(TextColor.color(HSVLike.fromRGB(255,255,255))))
                }
                26 -> {
                    skillName = "VerticalDash"
                    skillCategory = "Dash Alternativo"
                    material = Material.ENDER_EYE
                    displayName = Component.text("Dash Vertical").color(TextColor.color(HSVLike.fromRGB(255,255,255)))
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

    override fun switchSkill(uuid: UUID, skillCategory: String, skillName: String) {
        when (skillCategory) {
            "Arma Primária" -> {
                primaryWeapon[uuid] = skillName
            }

            "Arma Alternativa" -> {
                secondaryWeapon[uuid] = skillName
            }

            "Arma Secundária" -> {
                primaryAltWeapon[uuid] = skillName
            }

            "Dash Primário" -> {
                dash[uuid] = skillName
            }

            "Dash Alternativo" -> {
                dashAlt[uuid] = skillName
            }
        }
    }
}