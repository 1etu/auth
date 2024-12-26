package net.hywave.auth.models;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthPlayer {
    private final UUID uuid;
    private final String username;
    private String passwordHash;
    private String backupCodes;
    private int loginAttempts = 0;
    private boolean authenticated = false;
    private boolean registered = false;
    private long lastLogin;
    private String lastIp;
    private String twoFactorSecret;
    
    private static final Map<UUID, AuthPlayer> players = new HashMap<>();

    public AuthPlayer(UUID uuid, String username, String passwordHash) {
        this.uuid = uuid;
        this.username = username;
        this.passwordHash = passwordHash;
        this.backupCodes = "";
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public void setBackupCodes(List<String> codes) {
        this.backupCodes = String.join(",", codes);
    }
    
    public String getBackupCodes() {
        return backupCodes;
    }
    
    public int getLoginAttempts() {
        return loginAttempts;
    }
    
    public void incrementLoginAttempts() {
        this.loginAttempts++;
    }
    
    public void resetLoginAttempts() {
        this.loginAttempts = 0;
    }
    
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public boolean isRegistered() {
        return registered || passwordHash != null;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public static AuthPlayer getAuthPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public static void addAuthPlayer(AuthPlayer player) {
        players.put(player.getUuid(), player);
    }

    public static void removeAuthPlayer(UUID uuid) {
        players.remove(uuid);
    }

    public void setLastLogin(long lastLogin) { this.lastLogin = lastLogin; }
    public void setLastIp(String lastIp) { this.lastIp = lastIp; }
    public void set2FASecret(String secret) { this.twoFactorSecret = secret; }
    public long getLastLogin() { return lastLogin; }
    public String getLastIp() { return lastIp; }
    public String get2FASecret() { return twoFactorSecret; }

    public static AuthPlayer fromResultSet(ResultSet rs) throws SQLException {
        AuthPlayer player = new AuthPlayer(
            UUID.fromString(rs.getString("uuid")),
            rs.getString("username"),
            rs.getString("password")
        );
        player.setLastLogin(rs.getLong("last_login"));
        player.setLastIp(rs.getString("last_ip"));
        player.set2FASecret(rs.getString("two_factor_secret"));
        return player;
    }
} 