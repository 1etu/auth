package net.hywave.auth.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import net.hywave.auth.Auth;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@SuppressWarnings("deprecation")
public class TwoFactorManager {
    private static final Map<UUID, String> pendingSetup = new ConcurrentHashMap<>();
    private static final GoogleAuthenticator gAuth = new GoogleAuthenticator();
    
    public static void initiate2FASetup(Player player) {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey();
        pendingSetup.put(player.getUniqueId(), secret);
        
        String issuer = Auth.getInstance().getConfig().getString("server-name", "Hywave Network");
        String data = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", 
            issuer, player.getName(), secret, issuer);

        player.sendMessage("");
        player.sendMessage("");
        player.sendMessage("");
        
        player.sendMessage("§b1. §7Download Google Authenticator app on your phone.");
        player.sendMessage("§b2. §7Open the app with the QR code specially generated for you.");
        player.sendMessage("§b3. §7Enter the code shown in the app into the 'Verify' menu below.");
        
        showQRCode(player, data);
        
        player.sendMessage("§c4. §7Master Key (if QR scan fails): §f" + secret);
    }
    
    private static void showQRCode(Player player, String data) {
        MapView view = Bukkit.createMap(player.getWorld());
        view.getRenderers().clear();
        
        view.addRenderer(new MapRenderer() {
            private boolean rendered = false;
            private BitMatrix qrMatrix;
            
            @Override
            public void render(MapView map, MapCanvas canvas, Player player) {
                if (rendered) return;
                
                for (int x = 0; x < 128; x++) {
                    for (int y = 0; y < 128; y++) {
                        canvas.setPixel(x, y, (byte) 34);
                    }
                }
                
                try {
                    MultiFormatWriter writer = new MultiFormatWriter();
                    qrMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 100, 100);
                    
                    int offsetX = (128 - qrMatrix.getWidth()) / 2;
                    int offsetY = (128 - qrMatrix.getHeight()) / 2;
                    
                    for (int x = 0; x < qrMatrix.getWidth(); x++) {
                        for (int y = 0; y < qrMatrix.getHeight(); y++) {
                            if (qrMatrix.get(x, y)) {
                                canvas.setPixel(x + offsetX, y + offsetY, (byte) 119);
                            }
                        }
                    }
                    
                    
                    rendered = true;
                } catch (WriterException e) {
                    Logger.error("Failed to generate QR code", e);
                }
            }
        });
        
        ItemStack mapItem = new ItemStack(Material.MAP);
        mapItem.setDurability(view.getId());
        
        player.getInventory().setItem(1, mapItem);
    }
    
    public static boolean verifyAndEnable2FA(Player player, String code) {
        String secret = pendingSetup.get(player.getUniqueId());
        if (secret == null) {
            return false;
        }
        
        try {
            int inputCode = Integer.parseInt(code);
            if (gAuth.authorize(secret, inputCode)) {
                Auth.getInstance().getManagerFactory().getDatabaseManager()
                    .save2FASecret(player.getUniqueId(), secret);
                pendingSetup.remove(player.getUniqueId());
                return true;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        
        return false;
    }
    
    public static boolean verify2FACode(UUID uuid, String code) {
        String secret = Auth.getInstance().getManagerFactory().getDatabaseManager()
            .get2FASecret(uuid);
            
        if (secret == null) {
            return true;
        }
        
        try {
            int inputCode = Integer.parseInt(code);
            return gAuth.authorize(secret, inputCode);
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static void cleanup() {
        pendingSetup.clear();
    }
    
    public static boolean isPendingSetup(UUID uuid) {
        return pendingSetup.containsKey(uuid);
    }
} 