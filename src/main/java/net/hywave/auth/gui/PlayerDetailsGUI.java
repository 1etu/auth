package net.hywave.auth.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import net.hywave.auth.Auth;
import net.hywave.auth.models.AuthPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class PlayerDetailsGUI implements InventoryProvider {
    private final Auth plugin;
    private final AuthPlayer targetPlayer;

    public PlayerDetailsGUI(Auth plugin, AuthPlayer targetPlayer) {
        this.plugin = plugin;
        this.targetPlayer = targetPlayer;
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        contents.fillBorders(ClickableItem.empty(AuthStatsGUI.createGlassPane()));

        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        skullMeta.setOwner(targetPlayer.getUsername());
        skullMeta.setDisplayName("§e§l" + targetPlayer.getUsername());
        skullMeta.setLore(Arrays.asList(
            "§7Status: " + (plugin.getServer().getPlayer(targetPlayer.getUuid()) != null ? "§aOnline" : "§cOffline"),
            "§7Last Login: §f" + new Date(targetPlayer.getLastLogin()),
            "§7Last IP: §f" + targetPlayer.getLastIp(),
            "§72FA Enabled: " + (targetPlayer.get2FASecret() != null ? "§aYes" : "§cNo"),
            "",
            "§7Click to view more options"
        ));
        skull.setItemMeta(skullMeta);
        contents.set(1, 4, ClickableItem.of(
            createSkullItem(targetPlayer.getUsername(), Arrays.asList(
                "§7Click for admin actions"
            )), 
            e -> openAdminActions(player, targetPlayer)
        ));
    }

    @Override
    public void update(Player player, InventoryContents contents) {
    }

    private void openAdminActions(Player player, AuthPlayer targetPlayer) {
        SmartInventory.builder()
            .id("adminActions")
            .provider(new AdminActionGUI(plugin, targetPlayer))
            .size(3, 9)
            .title("§8Admin Actions: " + targetPlayer.getUsername())
            .manager(plugin.getInventoryManager())
            .build()
            .open(player);
    }

    private ItemStack createSkullItem(String owner, List<String> lore) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwner(owner);
        meta.setDisplayName("§e§l" + owner);
        meta.setLore(lore);
        skull.setItemMeta(meta);
        return skull;
    }
} 