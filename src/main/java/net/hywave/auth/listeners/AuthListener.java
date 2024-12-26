package net.hywave.auth.listeners;

import net.hywave.auth.Auth;
import net.hywave.auth.models.AuthPlayer;
import net.hywave.auth.utils.ScoreboardUtil;
import net.hywave.auth.utils.TimeoutBar;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Location;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class AuthListener implements Listener {
    private final Auth plugin;
    private final Map<UUID, TimeoutBar> timeoutBars = new HashMap<>();

    public AuthListener(Auth plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        
        player.getInventory().clear();
        player.setFoodLevel(20);
        player.setCanPickupItems(false);
        
        Location spawnLoc = player.getLocation();
        player.setWalkSpeed(0.0f);
        player.setFlySpeed(0.0f);
        player.setAllowFlight(false);
        
        // Clear chat with 100 empty messages
        for (int i = 0; i < 100; i++) {
            player.sendMessage("");
        }
        
        // Add effects to further restrict movement
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 99999999, 1, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 99999999, 128, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 99999999, 128, false, false));
        
        player.setPlayerListName("§k" + player.getName());
        
        plugin.getManagerFactory().getDatabaseManager().getPlayer(player.getUniqueId())
            .thenAccept(loadedPlayer -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.teleport(spawnLoc);
                    
                    if (loadedPlayer != null) {
                        AuthPlayer.addAuthPlayer(loadedPlayer);
                        player.sendMessage("§bAn account with this username exists!");
                        player.sendMessage("§7Please login using §b/login <password>");
                        player.sendMessage("");
                        
                        TextComponent backupCodeButton = new TextComponent("§6§l[I Have a Backup Code]");
                        backupCodeButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                            new ComponentBuilder("§7Click here to use your backup code").create()));
                        backupCodeButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/backupcode"));
                        player.spigot().sendMessage(backupCodeButton);
                        
                        player.sendTitle("", "§e/login <password>");
                        player.setMaxHealth(6.0);
                        player.setHealth(6.0);
                    } else {
                        player.sendMessage("§bNo account found with this username!");
                        player.sendMessage("§7Please register using §b/register <password>");
                        player.sendTitle("", "§e/register <password>");
                    }
                    
                    boolean isRegistered = loadedPlayer != null;
                    ScoreboardUtil.setAuthScoreboard(player, isRegistered);
                    
                    TimeoutBar timeoutBar = new TimeoutBar(plugin, player);
                    timeoutBars.put(player.getUniqueId(), timeoutBar);
                    
                    plugin.getSecurityListener().addUnregisteredPlayer(player);
                });
            });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
        
        AuthPlayer.removeAuthPlayer(uuid);
        TimeoutBar timeoutBar = timeoutBars.remove(uuid);
        if (timeoutBar != null) {
            timeoutBar.cancel();
        }
        plugin.getSecurityListener().authenticatePlayer(player);
    }

    public TimeoutBar getTimeoutBar(UUID uuid) {
        return timeoutBars.get(uuid);
    }

    public void restorePlayerMovement(Player player) {
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
        player.removePotionEffect(PotionEffectType.SLOW);
        player.removePotionEffect(PotionEffectType.JUMP);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
    }

    public void authenticatePlayer(Player player) {
        restorePlayerMovement(player);
        plugin.getSecurityListener().authenticatePlayer(player);
        ScoreboardUtil.removeScoreboard(player);
        
        TimeoutBar timeoutBar = getTimeoutBar(player.getUniqueId());
        if (timeoutBar != null) {
            timeoutBar.cancel();
        }
        
        player.setPlayerListName("§r" + player.getName());
        player.setMaxHealth(20.0);
        player.setHealth(20.0);
    }

    public void startAuthProcess(Player player) {
        plugin.getSecurityListener().addUnregisteredPlayer(player);
        ScoreboardUtil.setAuthScoreboard(player, false);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1));
        player.setPlayerListName("§7" + player.getName());
        new TimeoutBar(plugin, player);
    }

    public void removeTimeoutBar(UUID uuid) {
        timeoutBars.remove(uuid);
    }
} 