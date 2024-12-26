package net.hywave.auth.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BackupCodesMap {
    private static final ConcurrentHashMap<UUID, List<String>> backupCodes = new ConcurrentHashMap<>();

    public static void addCodes(UUID uuid, List<String> codes) {
        backupCodes.put(uuid, codes);
    }

    public static List<String> getCodes(UUID uuid) {
        return backupCodes.get(uuid);
    }

    public static void removeCodes(UUID uuid) {
        backupCodes.remove(uuid);
    }

    public static boolean validateCode(UUID uuid, String code) {
        List<String> codes = getCodes(uuid);
        if (codes == null) return false;
        
        boolean isValid = codes.contains(code);
        if (isValid) {
            codes.remove(code);
            if (codes.isEmpty()) {
                removeCodes(uuid);
            }
        }
        return isValid;
    }

    @SuppressWarnings("deprecation")
    public static void showBackupCodes(Player player, List<String> codes) {
        MapView view = Bukkit.createMap(player.getWorld());
        view.getRenderers().clear();
        
        view.addRenderer(new MapRenderer() {
            private boolean rendered = false;

            @Override
            public void render(MapView map, MapCanvas canvas, Player player) {
                if (rendered) return;
                
                for (int x = 0; x < 128; x++) {
                    for (int y = 0; y < 128; y++) {
                        canvas.setPixel(x, y, (byte) 34);
                    }
                }

                view.getRenderers().clear();

                
                int y = 40;
                for (String code : codes) {
                    canvas.drawText(10, y, MinecraftFont.Font, code);
                    y += 10;
                }
                
                rendered = true;
            }
        });

        ItemStack mapItem = new ItemStack(Material.MAP);
        mapItem.setDurability(view.getId());
        
        player.getInventory().setItem(0, mapItem);
        player.setItemInHand(mapItem);
    }
}