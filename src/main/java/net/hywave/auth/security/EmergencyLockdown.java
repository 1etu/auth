package net.hywave.auth.security;

import net.hywave.auth.Auth;
import net.hywave.auth.models.AuthEvent;
import net.hywave.auth.models.AuthEventType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class EmergencyLockdown {
    private final Auth plugin;
    private final AtomicBoolean lockdownActive = new AtomicBoolean(false);
    private final Set<UUID> exemptPlayers = new HashSet<>();
    private long lockdownStartTime;
    private String lockdownReason;

    public EmergencyLockdown(Auth plugin) {
        this.plugin = plugin;
    }

    public void activateLockdown(String reason, UUID initiator) {
        if (lockdownActive.compareAndSet(false, true)) {
            lockdownStartTime = Instant.now().getEpochSecond();
            lockdownReason = reason;
            
            plugin.getSessionManager().invalidateAllSessions();
            
            plugin.getServer().getOnlinePlayers().forEach(player -> {
                player.sendMessage("§c§lEMERGENCY LOCKDOWN ACTIVATED");
                player.sendMessage("§cReason: " + reason);
                plugin.getAuthListener().startAuthProcess(player);
            });
            
            plugin.getAuthLogger().logEvent(new AuthEvent(
                initiator,
                AuthEventType.EMERGENCY_LOCKDOWN,
                null,
                true,
                reason,
                null,
                System.currentTimeMillis()
            ));
        }
    }

    public void deactivateLockdown(UUID deactivator) {
        if (lockdownActive.compareAndSet(true, false)) {
            plugin.getAuthLogger().logEvent(new AuthEvent(
                deactivator,
                AuthEventType.EMERGENCY_LOCKDOWN,
                null,
                true,
                "Lockdown deactivated",
                null,
                System.currentTimeMillis()
            ));

            broadcastToExempt("§a[Emergency Lockdown] Deactivated");
            lockdownReason = null;
        }
    }

    public boolean isLockdownActive() {
        return lockdownActive.get();
    }

    public void addExemptPlayer(UUID uuid) {
        exemptPlayers.add(uuid);
    }

    public void removeExemptPlayer(UUID uuid) {
        exemptPlayers.remove(uuid);
        if (lockdownActive.get()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.kickPlayer("§c[Emergency Lockdown] Your exempt status has been revoked");
            }
        }
    }

    private void broadcastToExempt(String message) {
        exemptPlayers.stream()
            .map(Bukkit::getPlayer)
            .filter(player -> player != null && player.isOnline())
            .forEach(player -> player.sendMessage(message));
    }

    public String getLockdownReason() {
        return lockdownReason;
    }

    public long getLockdownDuration() {
        return lockdownActive.get() ? Instant.now().getEpochSecond() - lockdownStartTime : 0;
    }

    public Set<UUID> getExemptPlayers() {
        return Collections.unmodifiableSet(exemptPlayers);
    }
} 