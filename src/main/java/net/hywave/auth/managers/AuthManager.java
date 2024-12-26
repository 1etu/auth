package net.hywave.auth.managers;

public class AuthManager {
    @SuppressWarnings("unused")
    private final DatabaseManager databaseManager;
    
    public AuthManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
} 