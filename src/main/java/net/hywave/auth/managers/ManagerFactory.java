package net.hywave.auth.managers;

import net.hywave.auth.Auth;

public class ManagerFactory {
    private final Auth plugin;
    private DatabaseManager databaseManager;
    private AuthManager authManager;
    
    public ManagerFactory(Auth plugin) {
        this.plugin = plugin;
        initializeManagers();
    }
    
    private void initializeManagers() {
        this.databaseManager = new DatabaseManager(plugin);
        this.authManager = new AuthManager(databaseManager);
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public AuthManager getAuthManager() {
        return authManager;
    }
    
} 