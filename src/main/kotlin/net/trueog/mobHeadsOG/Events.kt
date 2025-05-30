package net.trueog.mobHeadsOG

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit.createProfile
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Creeper
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.persistence.PersistentDataType
import org.bukkit.profile.PlayerTextures
import java.util.*

class Events : Listener {
    val customMobHeadKey = NamespacedKey(MobHeadsOG.plugin, "customMobHead")
    val nullUuid: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        if (event.entity.killer !is Player) {
            return
        }

        val dropChance = if (event.entityType == EntityType.CREEPER && (event.entity as Creeper).isPowered) {
            MobHeadsOG.config.chargedCreeperChance
        } else {
            MobHeadsOG.config.dropChances[event.entityType]
        }

        if (dropChance == null) {
            return
        }

        if (!(Math.random() < dropChance)) {
            return
        }

        when (event.entityType) {
            EntityType.SKELETON -> {
                val skull = ItemStack(Material.SKELETON_SKULL, 1)
                event.drops.add(skull)
                return
            }

            EntityType.ZOMBIE -> {
                val head = ItemStack(Material.ZOMBIE_HEAD, 1)
                event.drops.add(head)
                return
            }

            EntityType.CREEPER -> {
                if (!(event.entity as Creeper).isPowered) {
                    val head = ItemStack(Material.CREEPER_HEAD, 1)
                    event.drops.add(head)
                    return
                }
            }

            else -> {}
        }

        val head = ItemStack(Material.PLAYER_HEAD, 1)
        val headMeta = head.itemMeta as SkullMeta

        val profile = createProfile(nullUuid)
        profile.clearProperties()
        val textures = profile.textures
        val skinUrl = Heads.getHeadUrl(event.entity)
        if (skinUrl == null) {
            return
        }

        val stringRepresentation = Heads.toStringRepresentation(event.entity)
        if (stringRepresentation == null) {
            return
        }

        headMeta.persistentDataContainer.set(customMobHeadKey, PersistentDataType.STRING, stringRepresentation)
        headMeta.displayName(
            Component.text("${Heads.getNameForEntity(event.entity)} Head").decoration(
                TextDecoration.ITALIC, false
            )
        )
        textures.setSkin(skinUrl, PlayerTextures.SkinModel.CLASSIC)
        profile.setTextures(textures)

        headMeta.playerProfile = profile
        head.itemMeta = headMeta
        event.drops.add(head)
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val customMobHeadData =
            event.itemInHand.itemMeta.persistentDataContainer.get(customMobHeadKey, PersistentDataType.STRING)
        if (customMobHeadData == null) {
            return
        }

        customMobHeadData == "CREEPER"

        event.block.setMetadata("customMobHead", FixedMetadataValue(MobHeadsOG.plugin, customMobHeadData))
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val blockType = event.block.type
        if (blockType != Material.PLAYER_HEAD && blockType != Material.PLAYER_WALL_HEAD) {
            return
        }

        val metadata = event.block.getMetadata("customMobHead")
        if (metadata.isEmpty()) {
            return
        }

        event.isCancelled = true
        event.block.removeMetadata("customMobHead", MobHeadsOG.plugin)

        val customMobHeadData = metadata.first().asString()

        val head = ItemStack(Material.PLAYER_HEAD, 1)
        val headMeta = head.itemMeta as SkullMeta

        val profile = createProfile(nullUuid)
        val textures = profile.textures
        val skinUrl = Heads.getHeadUrlWithStringRepresentation(customMobHeadData)
        if (skinUrl == null) {
            return
        }

        headMeta.persistentDataContainer.set(customMobHeadKey, PersistentDataType.STRING, customMobHeadData)
        headMeta.displayName(
            Component.text("${Heads.getNameForEntity(customMobHeadData)} Head").decoration(
                TextDecoration.ITALIC, false
            )
        )
        textures.setSkin(skinUrl, PlayerTextures.SkinModel.CLASSIC)
        profile.setTextures(textures)

        headMeta.playerProfile = profile
        head.itemMeta = headMeta


        event.block.world.dropItemNaturally(event.block.location, head)
        event.block.type = Material.AIR
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.isBlockInHand()) {
            return
        }

        val metadata = event.clickedBlock?.getMetadata("customMobHead") ?: listOf()
        if (metadata.isEmpty()) {
            return
        }

        val customMobHeadData = metadata.first().asString()
        val name = Heads.getNameForEntity(customMobHeadData)
        if (name == null) {
            return
        }

        event.player.sendActionBar(MobHeadsOG.mm.deserialize("<yellow>$name Head"))
    }
}