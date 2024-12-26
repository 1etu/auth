package net.hywave.auth.utils;

import net.hywave.auth.Auth;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.wesjd.anvilgui.AnvilGUI;
import java.util.Arrays;

public class BackupCodeGUI {
    private final Auth plugin;

    public BackupCodeGUI(Auth plugin) {
        this.plugin = plugin;
    }

    public void openBackupCodeGUI(Player player) {
        if (!plugin.getSecurityListener().isUnregistered(player)) {
            return;
        }

        new AnvilGUI.Builder()
            .onClick((slot, stateSnapshot) -> {
                if (!plugin.getSecurityListener().isUnregistered(player)) {
                    return Arrays.asList(AnvilGUI.ResponseAction.close());
                }
                
                String text = stateSnapshot.getText();
                if (plugin.getManagerFactory().getDatabaseManager().verifyBackupCode(player.getUniqueId(), text)) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§c§lWARNING: §eThis backup code has been used and is no longer valid.\n");
                        player.sendMessage("§eFor security reasons, please change your password using §b/changepassword§e.");
                        plugin.getAuthListener().authenticatePlayer(player);
                    });
                    return Arrays.asList(AnvilGUI.ResponseAction.close());
                } else {
                    return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("Invalid backup code!"));
                }
            })
            .text("Enter backup code")
            .itemLeft(new ItemStack(Material.PAPER))
            .title("Enter Backup Code")
            .plugin(plugin)
            .open(player);
    }
} 