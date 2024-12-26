package net.hywave.auth.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import net.hywave.auth.Auth;
import net.hywave.auth.models.AuthPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Arrays;

public class RegisteredPlayersGUI implements InventoryProvider {
    private final Auth plugin;
    private static final int PLAYERS_PER_PAGE = 21;
    private final int currentPage;

    public RegisteredPlayersGUI(Auth plugin, int currentPage) {
        this.plugin = plugin;
        this.currentPage = currentPage;
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        contents.fillBorders(ClickableItem.empty(AuthStatsGUI.createGlassPane()));
        Pagination pagination = contents.pagination();

        CompletableFuture.runAsync(() -> {
            List<AuthPlayer> players = plugin.getManagerFactory().getDatabaseManager()
                .getRegisteredPlayers(PLAYERS_PER_PAGE, currentPage * PLAYERS_PER_PAGE);
            @SuppressWarnings("unused")
            int totalPlayers = plugin.getManagerFactory().getDatabaseManager().getTotalPlayers();
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                ClickableItem[] items = new ClickableItem[players.size()];
                
                for (int i = 0; i < players.size(); i++) {
                    AuthPlayer authPlayer = players.get(i);
                    items[i] = ClickableItem.of(
                        createPlayerHead(authPlayer),
                        e -> openAdminActions(player, authPlayer)
                    );
                }

                pagination.setItems(items);
                pagination.setItemsPerPage(PLAYERS_PER_PAGE);

                if (currentPage > 0) {
                    contents.set(5, 3, ClickableItem.of(
                        createItem(Material.ARROW, "§ePrevious Page", null),
                        e -> openPage(player, currentPage - 1)
                    ));
                }

                if (players.size() == PLAYERS_PER_PAGE) {
                    contents.set(5, 5, ClickableItem.of(
                        createItem(Material.ARROW, "§eNext Page", null),
                        e -> openPage(player, currentPage + 1)
                    ));
                }

                contents.set(5, 4, ClickableItem.of(
                    createItem(Material.BARRIER, "§c§lBack", Arrays.asList("§7Return to statistics")),
                    e -> new AuthStatsGUI(plugin).show(player)
                ));

                pagination.addToIterator(contents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 1));
            });
        });
    }

    private void openPage(Player player, int page) {
        SmartInventory.builder()
            .id("registeredPlayers")
            .provider(new RegisteredPlayersGUI(plugin, page))
            .size(6, 9)
            .title("§8Registered Players - Page " + (page + 1))
            .manager(plugin.getInventoryManager())
            .build()
            .open(player);
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

    private ItemStack createPlayerHead(AuthPlayer player) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwner(player.getUsername());
        meta.setDisplayName("§e" + player.getUsername());
        meta.setLore(Arrays.asList(
            "§7Last Login: §f" + new java.util.Date(player.getLastLogin()),
            "§7Last IP: §f" + player.getLastIp(),
            "§72FA Enabled: " + (player.get2FASecret() != null ? "§aYes" : "§cNo"),
            "",
            "§eClick to manage player"
        ));
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void update(Player player, InventoryContents contents) {}
} 