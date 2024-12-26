package net.hywave.auth.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.security.SecureRandom;

public class CaptchaManager {
    private static final Map<UUID, String> playerCaptchas = new ConcurrentHashMap<>();
    private static final SecureRandom random = new SecureRandom();
    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    
    public static String generateCaptcha(Player player) {
        String captcha = generateCode();
        playerCaptchas.put(player.getUniqueId(), captcha);
        showCaptcha(player, captcha);
        return captcha;
    }
    
    public static boolean verifyCaptcha(UUID uuid, String input) {
        String captcha = playerCaptchas.get(uuid);
        if (captcha != null && captcha.equalsIgnoreCase(input)) {
            playerCaptchas.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removeMapItems(player);
            }
            return true;
        }
        return false;
    }
    
    private static String generateCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return code.toString();
    }
    
    @SuppressWarnings("deprecation")
    private static void showCaptcha(Player player, String captcha) {
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
                
                canvas.drawText(20, 30, MinecraftFont.Font, "VERIFICATION");
                canvas.drawText(20, 50, MinecraftFont.Font, captcha);
                canvas.drawText(20, 70, MinecraftFont.Font, "Type in chat");
                
                for (int i = 0; i < 100; i++) {
                    int x = random.nextInt(128);
                    int y = random.nextInt(128);
                    canvas.setPixel(x, y, (byte) (random.nextInt(4) + 32));
                }
                
                rendered = true;
            }
        });

        ItemStack mapItem = new ItemStack(Material.MAP);
        mapItem.setDurability(view.getId());
        
        player.getInventory().addItem(mapItem);
        player.setItemInHand(mapItem);
    }
    
    public static void cleanup() {
        playerCaptchas.clear();
    }
    
    private static void removeMapItems(Player player) {
        player.getInventory().forEach(item -> {
            if (item != null && item.getType() == Material.MAP) {
                item.setAmount(0);
            }
        });
        player.updateInventory();
    }
} 