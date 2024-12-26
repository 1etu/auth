package net.hywave.auth.logging;

import net.hywave.auth.Auth;
import net.hywave.auth.models.AuthEvent;
import net.hywave.auth.models.AuthEventType;
import net.hywave.auth.utils.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.HashSet;
import java.util.Set;

public class AuthLogger {
    private final Auth plugin;
    private static final int RETENTION_DAYS = 30;

    public AuthLogger(Auth plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = plugin.getManagerFactory().getDatabaseManager().getConnection()) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS auth_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "timestamp BIGINT NOT NULL, " +
                "player_uuid TEXT NOT NULL, " +
                "event_type TEXT NOT NULL, " +
                "ip_address TEXT, " +
                "success BOOLEAN NOT NULL, " +
                "details TEXT, " +
                "geo_location TEXT" +
                ")"
            );
            
            conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_auth_logs_timestamp ON auth_logs(timestamp)"
            );
            conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_auth_logs_player ON auth_logs(player_uuid)"
            );
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize auth logging: " + e.getMessage());
        }
    }

    public CompletableFuture<Void> logEvent(AuthEvent event) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getManagerFactory().getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO auth_logs (timestamp, player_uuid, event_type, ip_address, success, details, geo_location) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)"
                 )) {
                
                stmt.setLong(1, event.getTimestamp());
                stmt.setString(2, event.getPlayerUUID().toString());
                stmt.setString(3, event.getType().name());
                stmt.setString(4, event.getIpAddress());
                stmt.setBoolean(5, event.isSuccess());
                stmt.setString(6, event.getDetails());
                stmt.setString(7, event.getGeoLocation());
                
                stmt.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to log auth event: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<List<AuthEvent>> getRecentEvents(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<AuthEvent> events = new ArrayList<>();
            try (Connection conn = plugin.getManagerFactory().getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM auth_logs ORDER BY timestamp DESC LIMIT ?"
                 )) {
                
                stmt.setInt(1, limit);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    events.add(new AuthEvent(
                        UUID.fromString(rs.getString("player_uuid")),
                        AuthEventType.valueOf(rs.getString("event_type")),
                        rs.getString("ip_address"),
                        rs.getBoolean("success"),
                        rs.getString("details"),
                        rs.getString("geo_location"),
                        rs.getLong("timestamp")
                    ));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to fetch recent events: " + e.getMessage());
            }
            return events;
        });
    }

    public void cleanupOldLogs() {
        try (Connection conn = plugin.getManagerFactory().getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM auth_logs WHERE timestamp < ?"
             )) {
            
            long cutoffTime = Instant.now().minusSeconds(RETENTION_DAYS * 24 * 60 * 60).toEpochMilli();
            stmt.setLong(1, cutoffTime);
            int deleted = stmt.executeUpdate();
            
            if (deleted > 0) {
                plugin.getLogger().info("Cleaned up " + deleted + " old auth log entries");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to cleanup old logs: " + e.getMessage());
        }
    }

    public List<AuthEvent> getEvents(UUID playerUuid, int limit) {
        List<AuthEvent> events = new ArrayList<>();
        try (Connection conn = plugin.getManagerFactory().getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM auth_logs WHERE player_uuid = ? ORDER BY timestamp DESC LIMIT ?")) {
            stmt.setString(1, playerUuid.toString());
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                events.add(new AuthEvent(
                    UUID.fromString(rs.getString("player_uuid")),
                    AuthEventType.valueOf(rs.getString("event_type")),
                    rs.getString("ip_address"),
                    rs.getBoolean("success"),
                    rs.getString("details"),
                    rs.getString("geo_location"),
                    rs.getLong("timestamp")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fetch events for player " + playerUuid + ": " + e.getMessage());
        }
        return events;
    }

    public int getRecentEventCount(UUID uuid, String eventType, long timeframe) {
        try (Connection conn = plugin.getManagerFactory().getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM auth_logs WHERE player_uuid = ? AND event_type = ? AND timestamp > ?")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, eventType);
            stmt.setLong(3, System.currentTimeMillis() - timeframe);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            Logger.error("Failed to get recent event count", e);
            return 0;
        }
    }

    public Set<String> getRecentIPAddresses(UUID uuid, long timeframe) {
        Set<String> ips = new HashSet<>();
        try (Connection conn = plugin.getManagerFactory().getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT DISTINCT ip_address FROM auth_logs WHERE player_uuid = ? AND timestamp > ?")) {
            stmt.setString(1, uuid.toString());
            stmt.setLong(2, System.currentTimeMillis() - timeframe);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ips.add(rs.getString("ip_address"));
            }
        } catch (SQLException e) {
            Logger.error("Failed to get recent IP addresses", e);
        }
        return ips;
    }

    public int getAccountsFromIP(String ip, long timeframe) {
        try (Connection conn = plugin.getManagerFactory().getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(DISTINCT player_uuid) FROM auth_logs WHERE ip_address = ? AND timestamp > ?")) {
            stmt.setString(1, ip);
            stmt.setLong(2, System.currentTimeMillis() - timeframe);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            Logger.error("Failed to get accounts from IP", e);
            return 0;
        }
    }
} 