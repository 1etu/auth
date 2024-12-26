package net.hywave.auth.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import net.hywave.auth.Auth;
import net.hywave.auth.models.AuthPlayer;
import net.wesjd.anvilgui.AnvilGUI;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;

public class AdminActionGUI implements InventoryProvider {
    private final Auth plugin;
    private final AuthPlayer targetPlayer;

    public AdminActionGUI(Auth plugin, AuthPlayer targetPlayer) {
        this.plugin = plugin;
        this.targetPlayer = targetPlayer;
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        contents.fillBorders(ClickableItem.empty(createGlassPane()));

        contents.set(1, 2, ClickableItem.of(
            createItem(Material.BARRIER, "§c§lLock Account", 
                Arrays.asList(
                    "§7Current status: " + (plugin.getManagerFactory().getDatabaseManager().isAccountLocked(targetPlayer.getUuid()) ? "§cLocked" : "§aUnlocked"),
                    "",
                    "§eClick to toggle lock status"
                )),
            e -> {
                if (plugin.getManagerFactory().getDatabaseManager().isAccountLocked(targetPlayer.getUuid())) {
                    plugin.getManagerFactory().getDatabaseManager().unlockAccount(targetPlayer.getUuid());
                    player.sendMessage("§aAccount unlocked successfully!");
                } else {
                    plugin.getManagerFactory().getDatabaseManager().lockAccount(targetPlayer.getUuid());
                    player.sendMessage("§cAccount locked successfully!");
                }
                init(player, contents);
            }
        ));

        contents.set(1, 4, ClickableItem.of(
            createItem(Material.PAPER, "§e§lForce Password Reset", 
                Arrays.asList(
                    "§7Set a new password for this account",
                    "",
                    "§eClick to change password"
                )),
            e -> {
                new AnvilGUI.Builder()
                    .onClose(stateSnapshot -> init(player, contents))
                    .onClick((slot, stateSnapshot) -> {
                        String newPassword = stateSnapshot.getText();
                        if (newPassword.length() < 6) {
                            return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("§cToo short!"));
                        }
                        plugin.getManagerFactory().getDatabaseManager().updatePassword(targetPlayer.getUuid(), newPassword);
                        player.sendMessage("§aPassword changed successfully!");
                        return Arrays.asList(AnvilGUI.ResponseAction.close());
                    })
                    .text("Enter new password")
                    .itemLeft(new ItemStack(Material.PAPER))
                    .title("Set New Password")
                    .plugin(plugin)
                    .open(player);
            }
        ));

        contents.set(1, 6, ClickableItem.of(
            createItem(Material.TNT, "§4§lUnregister Account", 
                Arrays.asList(
                    "§c§lWARNING",
                    "§7This will completely remove",
                    "§7the account registration",
                    "",
                    "§eClick to unregister"
                )),
            e -> {
                plugin.getManagerFactory().getDatabaseManager().unregisterAccount(targetPlayer.getUuid());
                player.sendMessage("§cAccount unregistered successfully!");
                player.closeInventory();
            }
        ));

        contents.set(2, 3, ClickableItem.of(
            createItem(Material.REDSTONE, "§c§lDisable 2FA", 
                Arrays.asList(
                    "§7Current status: " + (targetPlayer.get2FASecret() != null ? "§aEnabled" : "§cDisabled"),
                    "",
                    "§eClick to disable 2FA"
                )),
            e -> {
                if (targetPlayer.get2FASecret() != null) {
                    plugin.getManagerFactory().getDatabaseManager().remove2FASecret(targetPlayer.getUuid());
                    player.sendMessage("§c2FA has been disabled for this account!");
                    init(player, contents);
                }
            }
        ));

        contents.set(2, 5, ClickableItem.of(
            createItem(Material.BOOK, "§b§lView Login History", 
                Arrays.asList(
                    "§7View recent login attempts",
                    "§7and security events",
                    "",
                    "§eClick to view"
                )),
            e -> {
                SmartInventory.builder()
                    .id("loginHistory")
                    .provider(new LoginHistoryGUI(plugin, targetPlayer, this))
                    .size(6, 9)
                    .title("§8Login History: " + targetPlayer.getUsername())
                    .manager(plugin.getInventoryManager())
                    .build()
                    .open(player);
            }
        ));
    }

    @Override
    public void update(Player player, InventoryContents contents) {}

    private ItemStack createItem(Material material, String name, java.util.List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGlassPane() {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }
} 