package net.hywave.auth.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import net.hywave.auth.Auth;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.UUID;

public class ExemptPlayersGUI implements InventoryProvider {
    private final Auth plugin;

    public ExemptPlayersGUI(Auth plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        contents.fillBorders(ClickableItem.empty(AuthStatsGUI.createGlassPane()));

        for (UUID uuid : plugin.getEmergencyLockdown().getExemptPlayers()) {
            Player exemptPlayer = Bukkit.getPlayer(uuid);
            if (exemptPlayer != null) {
                ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                meta.setOwner(exemptPlayer.getName());
                meta.setDisplayName("§e" + exemptPlayer.getName());
                meta.setLore(Arrays.asList("§7Click to remove exemption"));
                skull.setItemMeta(meta);

                contents.add(ClickableItem.of(skull, e -> {
                    plugin.getEmergencyLockdown().removeExemptPlayer(uuid);
                    player.sendMessage("§cRemoved " + exemptPlayer.getName() + " from exempt list");
                    init(player, contents);
                }));
            }
        }
    }

    @Override
    public void update(Player player, InventoryContents contents) {}
} 