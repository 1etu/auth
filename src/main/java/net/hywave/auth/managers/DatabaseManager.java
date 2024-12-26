package net.hywave.auth.managers;

import net.hywave.auth.Auth;
import net.hywave.auth.models.AuthPlayer;
import net.hywave.auth.utils.Logger;
import net.hywave.auth.utils.PasswordUtils;

import java.util.UUID;
import java.sql.*;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

public class DatabaseManager {
    private final Auth plugin;
    private final ExecutorService executor;
    private Connection connection;
    private boolean isConnected;
    
    public DatabaseManager(Auth plugin) {
        this.plugin = plugin;
        this.isConnected = false;
        this.executor = Executors.newFixedThreadPool(
            plugin.getConfig().getInt("database.connection-pool-size", 10)
        );
        initializeDatabase();
    }
    
    private synchronized void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdir();
            }
            
            String dbFile = new File(dataFolder, "database.db").getAbsolutePath();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
                stmt.execute("PRAGMA busy_timeout=30000");
            }
            
            createTables();
            isConnected = true;
            Logger.info("Database connection initialized successfully!");
        } catch (Exception e) {
            isConnected = false;
            Logger.error("Failed to initialize database connection", e);
        }
    }
    
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            initializeDatabase();
        }
        return connection;
    }
    
    public CompletableFuture<AuthPlayer> getPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> loadPlayer(uuid), executor);
    }
    
    public void disconnect() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                Logger.error("Error closing database connection", e);
            }
        }
    }
    
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS auth_players (" +
                "uuid TEXT PRIMARY KEY, " +
                "username TEXT NOT NULL, " +
                "password TEXT, " +
                "backup_codes TEXT, " +
                "registered_ip TEXT, " +
                "last_ip TEXT, " +
                "registration_date INTEGER, " +
                "last_login INTEGER, " +
                "two_factor_secret TEXT, " +
                "locked BOOLEAN DEFAULT 0, " +
                "force_reset BOOLEAN DEFAULT 0" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS auth_sessions (" +
                "uuid TEXT NOT NULL, " +
                "session_token TEXT NOT NULL, " +
                "ip_address TEXT NOT NULL, " +
                "client_token TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "last_activity INTEGER NOT NULL, " +
                "metadata TEXT, " +
                "PRIMARY KEY (uuid, session_token)" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS suspicious_activities (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "uuid TEXT NOT NULL, " +
                "ip_address TEXT NOT NULL, " +
                "activity_type TEXT NOT NULL, " +
                "details TEXT, " +
                "timestamp INTEGER NOT NULL, " +
                "severity INTEGER NOT NULL" +
                ")"
            );

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sessions_ip ON auth_sessions(ip_address)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sessions_last_activity ON auth_sessions(last_activity)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_suspicious_timestamp ON suspicious_activities(timestamp)");
        }
    }
    
    public void savePlayer(AuthPlayer player, String ip) {
        if (!isConnected) {
            Logger.error("Database is not connected!", new IllegalStateException());
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO auth_players " +
                "(uuid, username, password, backup_codes, registered_ip, last_ip, registration_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                stmt.setString(1, player.getUuid().toString());
                stmt.setString(2, player.getUsername());
                stmt.setString(3, player.getPasswordHash());
                stmt.setString(4, player.getBackupCodes());
                stmt.setString(5, ip);
                stmt.setString(6, ip);
                stmt.setLong(7, System.currentTimeMillis());
                
                stmt.executeUpdate();
                Logger.debug("Successfully saved data for " + player.getUsername());
            } catch (Exception e) {
                Logger.error("Failed to save player data for " + player.getUsername(), e);
            }
        }, executor);
    }
    
    public AuthPlayer loadPlayer(UUID uuid) {
        String sql = "SELECT * FROM auth_players WHERE uuid = ?";
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    AuthPlayer player = new AuthPlayer(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("username"),
                        rs.getString("password")
                    );
                    String backupCodesStr = rs.getString("backup_codes");
                    if (backupCodesStr != null && !backupCodesStr.isEmpty()) {
                        player.setBackupCodes(Arrays.asList(backupCodesStr.split(",")));
                    }
                    return player;
                }
            }
        } catch (Exception e) {
            Logger.error("Failed to load player data for UUID " + uuid, e);
        }
        return null;
    }
    
    public boolean verifyPassword(UUID uuid, String password) {
        AuthPlayer player = loadPlayer(uuid);
        if (player == null) {
            return false;
        }
        
        try {
            return PasswordUtils.checkPassword(password, player.getPasswordHash());
        } catch (IllegalArgumentException e) {
            Logger.warn("Invalid password hash format for player " + uuid);
            return false;
        }
    }
    
    public boolean verifyBackupCode(UUID uuid, String code) {
        AuthPlayer player = loadPlayer(uuid);
        if (player == null || player.getBackupCodes() == null) {
            return false;
        }
        
        String[] backupCodes = player.getBackupCodes().split(",");
        for (String backupCode : backupCodes) {
            if (backupCode.equals(code)) {
                List<String> remainingCodes = new ArrayList<>(Arrays.asList(backupCodes));
                remainingCodes.remove(code);
                player.setBackupCodes(remainingCodes);
                savePlayer(player, null);
                return true;
            }
        }
        return false;
    }
    
    public AuthPlayer getAuthPlayer(UUID uuid) {
        try {
            String sql = "SELECT * FROM auth_players WHERE uuid = ?";
            try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    AuthPlayer player = new AuthPlayer(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("username"),
                        rs.getString("password")
                    );
                    return player;
                }
            }
        } catch (SQLException e) {
            Logger.error("Failed to get auth player data", e);
        }
        return null;
    }
    
    public void save2FASecret(UUID uuid, String secret) {
        String sql = "UPDATE auth_players SET two_factor_secret = ? WHERE uuid = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, secret);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Failed to save 2FA secret for " + uuid, e);
        }
    }
    
    public String get2FASecret(UUID uuid) {
        String sql = "SELECT two_factor_secret FROM auth_players WHERE uuid = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("two_factor_secret");
            }
        } catch (SQLException e) {
            Logger.error("Failed to get 2FA secret for " + uuid, e);
        }
        return null;
    }
    
    public int getTotalPlayers() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM auth_players")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            Logger.error("Failed to get total players count", e);
        }
        return 0;
    }
    
    public List<AuthPlayer> searchPlayers(String query) {
        List<AuthPlayer> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM auth_players WHERE username LIKE ? LIMIT 100")) {
            stmt.setString(1, "%" + query + "%");
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                AuthPlayer player = new AuthPlayer(
                    UUID.fromString(rs.getString("uuid")),
                    rs.getString("username"),
                    rs.getString("password")
                );
                player.setLastLogin(rs.getLong("registration_date"));
                player.setLastIp(rs.getString("last_ip"));
                player.set2FASecret(rs.getString("two_factor_secret"));
                results.add(player);
            }
        } catch (SQLException e) {
            Logger.error("Failed to search players", e);
        }
        return results;
    }
    
    public void lockAccount(UUID uuid) {
        String sql = "UPDATE auth_players SET locked = 1 WHERE uuid = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Failed to lock account for " + uuid, e);
        }
    }
    
    public void unlockAccount(UUID uuid) {
        String sql = "UPDATE auth_players SET locked = 0 WHERE uuid = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Failed to unlock account for " + uuid, e);
        }
    }
    
    public boolean isAccountLocked(UUID uuid) {
        String sql = "SELECT locked FROM auth_players WHERE uuid = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("locked");
            }
        } catch (SQLException e) {
            Logger.error("Failed to check account lock status for " + uuid, e);
        }
        return false;
    }
    
    public void setForcePasswordReset(UUID uuid, boolean required) {
        String sql = "UPDATE auth_players SET force_reset = ? WHERE uuid = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setBoolean(1, required);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Failed to set force password reset for " + uuid, e);
        }
    }
    
    public void unregisterAccount(UUID uuid) {
        String sql = "DELETE FROM auth_players WHERE uuid = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Failed to unregister account for " + uuid, e);
        }
    }
    
    public void remove2FASecret(UUID uuid) {
        String sql = "UPDATE auth_players SET two_factor_secret = NULL WHERE uuid = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Failed to remove 2FA secret for " + uuid, e);
        }
    }
    
    public void updatePassword(UUID uuid, String newPassword) {
        String sql = "UPDATE auth_players SET password = ? WHERE uuid = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, PasswordUtils.hashPassword(newPassword));
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Failed to update password for " + uuid, e);
        }
    }
    
    public List<AuthPlayer> getRegisteredPlayers(int limit, int offset) {
        List<AuthPlayer> players = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM auth_players ORDER BY last_login DESC LIMIT ? OFFSET ?")) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                players.add(AuthPlayer.fromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return players;
    }
} 
