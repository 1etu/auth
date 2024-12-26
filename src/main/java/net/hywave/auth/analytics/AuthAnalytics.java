package net.hywave.auth.analytics;

import net.hywave.auth.Auth;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AuthAnalytics {
    private final Auth plugin;

    public AuthAnalytics(Auth plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Map<String, Integer>> getLoginStatistics(int hours) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> stats = new HashMap<>();
            try (Connection conn = plugin.getManagerFactory().getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT " +
                     "COUNT(*) as total_attempts, " +
                     "SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as successful_attempts, " +
                     "SUM(CASE WHEN event_type = 'TWO_FACTOR_ATTEMPT' THEN 1 ELSE 0 END) as 2fa_attempts, " +
                     "SUM(CASE WHEN event_type = 'BACKUP_CODE_USED' THEN 1 ELSE 0 END) as backup_code_uses " +
                     "FROM auth_logs " +
                     "WHERE timestamp > ? AND event_type IN ('LOGIN_ATTEMPT', 'TWO_FACTOR_ATTEMPT', 'BACKUP_CODE_USED')"
                 )) {
                
                long cutoffTime = Instant.now().minusSeconds(hours * 3600).toEpochMilli();
                stmt.setLong(1, cutoffTime);
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    stats.put("total_attempts", rs.getInt("total_attempts"));
                    stats.put("successful_attempts", rs.getInt("successful_attempts"));
                    stats.put("2fa_attempts", rs.getInt("2fa_attempts"));
                    stats.put("backup_code_uses", rs.getInt("backup_code_uses"));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to fetch login statistics: " + e.getMessage());
            }
            return stats;
        });
    }

    public CompletableFuture<Map<String, Integer>> getSuspiciousActivityReport() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> report = new HashMap<>();
            try (Connection conn = plugin.getManagerFactory().getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT " +
                     "COUNT(DISTINCT player_uuid) as affected_players, " +
                     "COUNT(DISTINCT ip_address) as unique_ips, " +
                     "SUM(CASE WHEN event_type = 'ACCOUNT_LOCKOUT' THEN 1 ELSE 0 END) as lockouts, " +
                     "SUM(CASE WHEN event_type = 'SUSPICIOUS_IP' THEN 1 ELSE 0 END) as suspicious_ips " +
                     "FROM auth_logs " +
                     "WHERE timestamp > ? AND (success = 0 OR event_type IN ('ACCOUNT_LOCKOUT', 'SUSPICIOUS_IP'))"
                 )) {
                
                long cutoffTime = Instant.now().minusSeconds(24 * 3600).toEpochMilli();
                stmt.setLong(1, cutoffTime);
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    report.put("affected_players", rs.getInt("affected_players"));
                    report.put("unique_ips", rs.getInt("unique_ips"));
                    report.put("lockouts", rs.getInt("lockouts"));
                    report.put("suspicious_ips", rs.getInt("suspicious_ips"));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to generate suspicious activity report: " + e.getMessage());
            }
            return report;
        });
    }

    public CompletableFuture<Map<Integer, Integer>> getHourlyLoginStats(int hours) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Integer, Integer> hourlyStats = new HashMap<>();
            try (Connection conn = plugin.getManagerFactory().getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT strftime('%H', datetime(timestamp/1000, 'unixepoch')) as hour, " +
                     "COUNT(*) as count FROM auth_logs " +
                     "WHERE timestamp > ? AND event_type = 'LOGIN_ATTEMPT' " +
                     "GROUP BY hour ORDER BY hour"
                 )) {
                
                long cutoffTime = Instant.now().minusSeconds(hours * 3600).toEpochMilli();
                stmt.setLong(1, cutoffTime);
                
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    hourlyStats.put(rs.getInt("hour"), rs.getInt("count"));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to fetch hourly login statistics: " + e.getMessage());
            }
            return hourlyStats;
        });
    }
} 