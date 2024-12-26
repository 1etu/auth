package net.hywave.auth.security;

import net.hywave.auth.Auth;
import net.hywave.auth.models.AuthEvent;
import net.hywave.auth.models.AuthEventType;
import net.hywave.auth.utils.Logger;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SessionManager {
    private final Auth plugin;
    private final ConcurrentHashMap<UUID, SessionData> sessions;
    private final SecureRandom secureRandom;
    private static final int SESSION_TIMEOUT_MINUTES = 30;
    private static final int MAX_SESSIONS_PER_IP = 3;
    private static final int SUSPICIOUS_SESSION_THRESHOLD = 5;

    private class SessionData {
        @SuppressWarnings("unused")
        String sessionToken;
        String ipAddress;
        String clientToken;
        long lastActivity;
        Map<String, Object> metadata;
        
        SessionData(String ip, String clientToken) {
            this.sessionToken = generateSessionToken();
            this.ipAddress = ip;
            this.clientToken = clientToken;
            this.lastActivity = Instant.now().getEpochSecond();
            this.metadata = new HashMap<>();
        }
    }

    public SessionManager(Auth plugin) {
        this.plugin = plugin;
        this.sessions = new ConcurrentHashMap<>();
        this.secureRandom = new SecureRandom();
        
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, 
            this::cleanupExpiredSessions, 20L * 60, 20L * 60);
    }

    private String generateSessionToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateClientToken(Player player) {
        try {
            String input = player.getUniqueId() + "|" + 
                          player.getAddress().getAddress().getHostAddress() + "|" +
                          player.getName() + "|" +
                          System.currentTimeMillis();
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            Logger.error("Failed to generate client token", e);
            return null;
        }
    }

    public boolean createSession(Player player) {
        String ip = player.getAddress().getAddress().getHostAddress();
        
        if (isSuspiciousActivity(player, ip)) {
            Logger.warn("Suspicious session creation attempt detected for " + player.getName() + " from " + ip);
            plugin.getAuthLogger().logEvent(new AuthEvent(
                player.getUniqueId(),
                AuthEventType.SUSPICIOUS_SESSION,
                ip,
                false,
                "Suspicious session attempt",
                null,
                System.currentTimeMillis()
            ));
            return false;
        }

        if (getSessionsFromIP(ip) >= MAX_SESSIONS_PER_IP) {
            Logger.warn("Too many sessions from IP: " + ip);
            return false;
        }

        String clientToken = generateClientToken(player);
        if (clientToken == null) return false;

        SessionData session = new SessionData(ip, clientToken);
        sessions.put(player.getUniqueId(), session);
        
        plugin.getAuthLogger().logEvent(new AuthEvent(
            player.getUniqueId(),
            AuthEventType.SESSION_CREATED,
            ip,
            true,
            "Session created",
            null,
            System.currentTimeMillis()
        ));
        
        return true;
    }

    private boolean isSuspiciousActivity(Player player, String ip) {
        try {
            int recentSessions = plugin.getAuthLogger().getRecentEventCount(
                player.getUniqueId(), "SESSION_CREATED", TimeUnit.MINUTES.toMillis(5));
            if (recentSessions > SUSPICIOUS_SESSION_THRESHOLD) {
                return true;
            }

            Set<String> recentIPs = plugin.getAuthLogger().getRecentIPAddresses(
                player.getUniqueId(), TimeUnit.HOURS.toMillis(24));
            if (recentIPs.size() > 5) {
                return true;
            }

            int accountsFromIP = plugin.getAuthLogger().getAccountsFromIP(ip, TimeUnit.HOURS.toMillis(24));
            if (accountsFromIP > MAX_SESSIONS_PER_IP * 2) {
                return true;
            }

            return false;
        } catch (Exception e) {
            Logger.error("Error checking suspicious activity", e);
            return true;
        }
    }

    public boolean validateSession(Player player) {
        SessionData session = sessions.get(player.getUniqueId());
        if (session == null) return false;

        String currentIP = player.getAddress().getAddress().getHostAddress();
        String currentClientToken = generateClientToken(player);

        boolean valid = session.ipAddress.equals(currentIP) &&
                       session.clientToken.equals(currentClientToken) &&
                       (Instant.now().getEpochSecond() - session.lastActivity) < TimeUnit.MINUTES.toSeconds(SESSION_TIMEOUT_MINUTES);

        if (valid) {
            session.lastActivity = Instant.now().getEpochSecond();
        } else {
            invalidateSession(player.getUniqueId());
            plugin.getAuthLogger().logEvent(new AuthEvent(
                player.getUniqueId(),
                AuthEventType.SESSION_VALIDATION_FAILED,
                currentIP,
                false,
                "Session validation failed",
                null,
                System.currentTimeMillis()
            ));
        }

        return valid;
    }

    public void invalidateSession(UUID uuid) {
        SessionData removed = sessions.remove(uuid);
        if (removed != null) {
            plugin.getAuthLogger().logEvent(new AuthEvent(
                uuid,
                AuthEventType.SESSION_INVALIDATED,
                removed.ipAddress,
                true,
                "Session invalidated",
                null,
                System.currentTimeMillis()
            ));
        }
    }

    public void invalidateAllSessions() {
        sessions.clear();
        Logger.info("All sessions have been invalidated");
    }

    private void cleanupExpiredSessions() {
        long now = Instant.now().getEpochSecond();
        sessions.entrySet().removeIf(entry -> 
            (now - entry.getValue().lastActivity) >= TimeUnit.MINUTES.toSeconds(SESSION_TIMEOUT_MINUTES));
    }

    private long getSessionsFromIP(String ip) {
        return sessions.values().stream()
            .filter(session -> session.ipAddress.equals(ip))
            .count();
    }

    public void updateSessionMetadata(UUID uuid, String key, Object value) {
        SessionData session = sessions.get(uuid);
        if (session != null) {
            session.metadata.put(key, value);
        }
    }
} 