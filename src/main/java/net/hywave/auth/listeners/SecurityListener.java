package net.hywave.auth.listeners;

import net.hywave.auth.Auth;
import net.hywave.auth.utils.ScoreboardUtil;
import net.hywave.auth.utils.RateLimiter;
import net.hywave.auth.utils.CaptchaManager;
import net.hywave.auth.utils.TimeoutBar;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public class SecurityListener implements Listener {
    private final Auth plugin;
    private final Set<UUID> unauthenticatedPlayers;
    private final Set<String> allowedCommands;
    private final Map<UUID, TimeoutBar> timeoutBars;

    public SecurityListener(Auth plugin) {
        this.plugin = plugin;
        this.unauthenticatedPlayers = new HashSet<>();
        this.allowedCommands = new HashSet<>();
        this.timeoutBars = new HashMap<>();
        
        allowedCommands.add("/login");
        allowedCommands.add("/register");
        allowedCommands.add("/help");
        allowedCommands.add("/backupcode");
    }

    public void addUnregisteredPlayer(Player player) {
        unauthenticatedPlayers.add(player.getUniqueId());
        timeoutBars.put(player.getUniqueId(), new TimeoutBar(plugin, player));
    }

    public void authenticatePlayer(Player player) {
        unauthenticatedPlayers.remove(player.getUniqueId());
        ScoreboardUtil.removeScoreboard(player);
        
        TimeoutBar timeoutBar = timeoutBars.remove(player.getUniqueId());
        if (timeoutBar != null) {
            timeoutBar.stopCountdown();
        }
    }

    public boolean isUnregistered(Player player) {
        return unauthenticatedPlayers.contains(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isUnregistered(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isUnregistered(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isUnregistered(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player && 
            isUnregistered((Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isUnregistered(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (isUnregistered(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && isUnregistered((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player && isUnregistered((Player) event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String ip = player.getAddress().getAddress().getHostAddress();
        
        if (RateLimiter.isCaptchaRequired(ip)) {
            event.setCancelled(true);
            
            if (CaptchaManager.verifyCaptcha(player.getUniqueId(), event.getMessage())) {
                player.sendMessage("§aCaptcha verified! You can now try to login again.");
                RateLimiter.clearAttempts(ip, player.getUniqueId());
            } else {
                RateLimiter.recordFailedAttempt(ip, player.getUniqueId());
                if (RateLimiter.isIpBlocked(ip)) {
                    player.kickPlayer("§cToo many failed attempts. Please try again in 15 minutes.");
                } else {
                    player.sendMessage("§cIncorrect captcha! Please try again.");
                }
            }
        }
    }

   

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getSessionManager().validateSession(player)) {
            plugin.getAuthListener().startAuthProcess(player);
        }
    }

    public TimeoutBar getTimeoutBar(UUID playerId) {
        return timeoutBars.get(playerId);
    }
} 