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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AuthStatsGUI implements InventoryProvider {
    private final Auth plugin;
    
    private SmartInventory inventory;

    public AuthStatsGUI(Auth plugin) {
        this.plugin = plugin;
        this.inventory = SmartInventory.builder()
                .id("authStats")
                .provider(this)
                .size(6, 9)
                .title("§8Authentication Statistics")
                .manager(plugin.getInventoryManager())
                .build();
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        contents.fillBorders(ClickableItem.empty(createGlassPane()));
        
        CompletableFuture.runAsync(() -> {
            Map<String, Integer> stats = plugin.getAuthAnalytics().getLoginStatistics(24).join();
            int totalPlayers = plugin.getManagerFactory().getDatabaseManager().getTotalPlayers();
            int onlinePlayers = plugin.getServer().getOnlinePlayers().size();
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                int twoFaAttempts = stats.getOrDefault("2fa_attempts", 0);
                int totalAttempts = stats.getOrDefault("total_attempts", 0);
                int successfulAttempts = stats.getOrDefault("successful_attempts", 0);
                int lockouts = stats.getOrDefault("lockouts", 0);
                int suspiciousIps = stats.getOrDefault("suspicious_ips", 0);
                int affectedPlayers = stats.getOrDefault("affected_players", 0);

                contents.set(1, 2, ClickableItem.of(createInfoItem(
                    Material.BOOK,
                    "§e§lTotal Players",
                    Arrays.asList(
                        "§7Total registered players:",
                        "§f" + totalPlayers,
                        "",
                        "§eClick to view all players"
                    )
                ), e -> openRegisteredPlayersGUI(player)));

                contents.set(1, 4, ClickableItem.empty(createInfoItem(
                    Material.EMERALD,
                    "§a§lOnline Players",
                    Arrays.asList(
                        "§7Currently online players:",
                        "§f" + onlinePlayers
                    )
                )));

                contents.set(1, 6, ClickableItem.empty(createInfoItem(
                    Material.TRIPWIRE_HOOK,
                    "§b§l2FA Statistics",
                    Arrays.asList(
                        "§7Players with 2FA enabled:",
                        "§f" + twoFaAttempts,
                        "",
                        "§7Last 24h attempts:",
                        "§f" + totalAttempts
                    )
                )));

                // Security Stats
                contents.set(3, 3, ClickableItem.empty(createInfoItem(
                    Material.BARRIER,
                    "§c§lFailed Attempts",
                    Arrays.asList(
                        "§7Failed login attempts (24h):",
                        "§f" + (totalAttempts - successfulAttempts),
                        "",
                        "§7Account lockouts:",
                        "§f" + lockouts
                    )
                )));

                contents.set(3, 5, ClickableItem.empty(createInfoItem(
                    Material.REDSTONE_COMPARATOR,
                    "§6§lSecurity Overview",
                    Arrays.asList(
                        "§7Suspicious IPs detected:",
                        "§f" + suspiciousIps,
                        "",
                        "§7Affected accounts:",
                        "§f" + affectedPlayers
                    )
                )));
            });
        });

        contents.set(4, 4, ClickableItem.of(createInfoItem(
            Material.COMPASS,
            "§e§lSearch Players",
            Arrays.asList("§7Click to search for players")
        ), e -> openSearchGUI(player)));

        contents.set(4, 2, ClickableItem.of(createInfoItem(
            Material.REDSTONE_BLOCK,
            "§c§lEmergency Lockdown",
            Arrays.asList(
                "§7Status: " + (plugin.getEmergencyLockdown().isLockdownActive() ? "§cActive" : "§aInactive"),
                "§7Duration: " + formatDuration(plugin.getEmergencyLockdown().getLockdownDuration()),
                "",
                "§eClick to toggle lockdown"
            )
        ), e -> {
            if (plugin.getEmergencyLockdown().isLockdownActive()) {
                plugin.getEmergencyLockdown().deactivateLockdown(player.getUniqueId());
                player.sendMessage("§aEmergency lockdown deactivated!");
            } else {
                new AnvilGUI.Builder()
                    .onClick((slot, stateSnapshot) -> {
                        String reason = stateSnapshot.getText();
                        plugin.getEmergencyLockdown().activateLockdown(reason, player.getUniqueId());
                        player.sendMessage("§cEmergency lockdown activated!");
                        return Arrays.asList(AnvilGUI.ResponseAction.close());
                    })
                    .text("Enter lockdown reason")
                    .itemLeft(new ItemStack(Material.PAPER))
                    .title("Lockdown Reason")
                    .plugin(plugin)
                    .open(player);
            }
        }));

        contents.set(4, 6, ClickableItem.of(createInfoItem(
            Material.GOLDEN_APPLE,
            "§6§lManage Exempt Players",
            Arrays.asList(
                "§7Click to manage players",
                "§7exempt from lockdown"
            )
        ), e -> openExemptPlayersGUI(player)));
    }

    @Override
    public void update(Player player, InventoryContents contents) {
    }

    private void openSearchGUI(Player player) {
        new AnvilGUI.Builder()
            .onClose(stateSnapshot -> {})
            .onClick((slot, stateSnapshot) -> {
                if (slot != AnvilGUI.Slot.OUTPUT) {
                    return Arrays.asList();
                }
                
                String query = stateSnapshot.getText();
                showPlayerDetails(player, query);
                return Arrays.asList(AnvilGUI.ResponseAction.close());
            })
            .text("Enter player name")
            .itemLeft(new ItemStack(Material.PAPER))
            .title("Search Players")
            .plugin(plugin)
            .open(player);
    }

    private void showPlayerDetails(Player player, String query) {
        CompletableFuture.runAsync(() -> {
            List<AuthPlayer> results = plugin.getManagerFactory().getDatabaseManager().searchPlayers(query);
            
            if (results.isEmpty()) {
                player.sendMessage("§cNo players found matching that query!");
                return;
            }

            AuthPlayer foundPlayer = results.get(0);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                SmartInventory.builder()
                    .id("playerDetails")
                    .provider(new PlayerDetailsGUI(plugin, foundPlayer))
                    .size(3, 9)
                    .title("§8Player Details: " + foundPlayer.getUsername())
                    .manager(plugin.getInventoryManager())
                    .build()
                    .open(player);
            });
        });
    }

    private ItemStack createInfoItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createGlassPane() {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    public void show(Player player) {
        inventory.open(player);
    }

    private String formatDuration(long seconds) {
        if (seconds == 0) return "N/A";
        long minutes = seconds / 60;
        long hours = minutes / 60;
        minutes %= 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    private void openExemptPlayersGUI(Player player) {
        SmartInventory.builder()
            .id("exemptPlayers")
            .provider(new ExemptPlayersGUI(plugin))
            .size(3, 9)
            .title("§8Exempt Players")
            .manager(plugin.getInventoryManager())
            .build()
            .open(player);
    }

    private void openRegisteredPlayersGUI(Player player) {
        SmartInventory.builder()
            .id("registeredPlayers")
            .provider(new RegisteredPlayersGUI(plugin, 0))
            .size(6, 9)
            .title("§8Registered Players")
            .manager(plugin.getInventoryManager())
            .build()
            .open(player);
    }
} 