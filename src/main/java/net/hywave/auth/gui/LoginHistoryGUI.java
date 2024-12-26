package net.hywave.auth.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import net.hywave.auth.Auth;
import net.hywave.auth.models.AuthEvent;
import net.hywave.auth.models.AuthPlayer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class LoginHistoryGUI implements InventoryProvider {
    private final Auth plugin;
    private final AuthPlayer targetPlayer;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final AdminActionGUI previousMenu;

    public LoginHistoryGUI(Auth plugin, AuthPlayer targetPlayer, AdminActionGUI previousMenu) {
        this.plugin = plugin;
        this.targetPlayer = targetPlayer;
        this.previousMenu = previousMenu;
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        contents.fillBorders(ClickableItem.empty(createGlassPane()));
        Pagination pagination = contents.pagination();

        List<AuthEvent> events = plugin.getAuthLogger().getEvents(targetPlayer.getUuid(), 100);
        ClickableItem[] items = new ClickableItem[events.size()];

        for (int i = 0; i < events.size(); i++) {
            AuthEvent event = events.get(i);
            Material material = getEventMaterial(event);
            String status = event.isSuccess() ? "§aSuccess" : "§cFailed";
            
            items[i] = ClickableItem.empty(createItem(material,
                "§e" + event.getType().toString(),
                Arrays.asList(
                    "§7Time: §f" + dateFormat.format(new Date(event.getTimestamp())),
                    "§7Status: " + status,
                    "§7IP: §f" + (event.getIpAddress() != null ? event.getIpAddress() : "N/A"),
                    "§7Location: §f" + (event.getGeoLocation() != null ? event.getGeoLocation() : "N/A"),
                    "",
                    "§7" + event.getDetails()
                )
            ));
        }

        pagination.setItems(items);
        pagination.setItemsPerPage(21); 

        if (!pagination.isFirst()) {
            contents.set(5, 3, ClickableItem.of(
                createItem(Material.ARROW, "§ePrevious Page", null),
                e -> init(player, contents)
            ));
        }

        if (!pagination.isLast()) {
            contents.set(5, 5, ClickableItem.of(
                createItem(Material.ARROW, "§eNext Page", null),
                e -> init(player, contents)
            ));
        }

        contents.set(5, 4, ClickableItem.of(
            createItem(Material.BARRIER, "§c§lBack", Arrays.asList("§7Return to admin actions")),
            e -> SmartInventory.builder()
                .id("adminActions")
                .provider(previousMenu)
                .size(3, 9)
                .title("§8Admin Actions: " + targetPlayer.getUsername())
                .manager(plugin.getInventoryManager())
                .build()
                .open(player)
        ));

        pagination.addToIterator(contents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 1));
    }

    private Material getEventMaterial(AuthEvent event) {
        switch (event.getType()) {
            case LOGIN_ATTEMPT:
                return event.isSuccess() ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
            case REGISTRATION:
                return Material.BOOK_AND_QUILL;
            case PASSWORD_CHANGE:
                return Material.PAPER;
            case BACKUP_CODE_USED:
                return Material.TRIPWIRE_HOOK;
            case TWO_FACTOR_ATTEMPT:
                return Material.COMPASS;
            case ACCOUNT_LOCKOUT:
                return Material.BARRIER;
            case EMERGENCY_ACCESS:
                return Material.GOLDEN_APPLE;
            case SUSPICIOUS_IP:
                return Material.REDSTONE_TORCH_ON;
            default:
                return Material.BOOK;
        }
    }

    @Override
    public void update(Player player, InventoryContents contents) {}

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
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