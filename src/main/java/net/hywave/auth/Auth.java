package net.hywave.auth;

import org.bukkit.plugin.java.JavaPlugin;
import net.hywave.auth.managers.ManagerFactory;
import net.hywave.auth.commands.RegisterCommand;
import net.hywave.auth.commands.LoginCommand;
import net.hywave.auth.listeners.AuthListener;
import net.hywave.auth.listeners.SecurityListener;
import net.hywave.auth.utils.Logger;
import net.hywave.auth.commands.BackupCodeCommand;
import net.hywave.auth.utils.RateLimiter;
import net.hywave.auth.utils.WebServer;
import net.hywave.auth.utils.CaptchaManager;
import net.hywave.auth.commands.Setup2FACommand;
import net.hywave.auth.logging.AuthLogger;
import net.hywave.auth.security.EmergencyLockdown;
import net.hywave.auth.analytics.AuthAnalytics;
import net.hywave.auth.commands.EmergencyLockdownCommand;
import net.hywave.auth.commands.AuthStatsCommand;
import fr.minuskube.inv.InventoryManager;
import net.hywave.auth.security.SessionManager;
public class Auth extends JavaPlugin {
    private static Auth instance;
    private ManagerFactory managerFactory;
    private SecurityListener securityListener;
    private AuthListener authListener;
    private AuthLogger authLogger;
    private EmergencyLockdown emergencyLockdown;
    private AuthAnalytics authAnalytics;
    private InventoryManager inventoryManager;
    private net.hywave.auth.security.SessionManager sessionManager;
    
    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        Logger.init(this);
        Logger.info("Initializing Auth plugin...");
        
        if (getConfig().getBoolean("web-server.enabled", true)) {
            WebServer.initialize(this);
        }
        
        
        this.managerFactory = new ManagerFactory(this);
        this.securityListener = new SecurityListener(this);
        this.authListener = new AuthListener(this);
        
        getCommand("register").setExecutor(new RegisterCommand(this));
        getCommand("login").setExecutor(new LoginCommand(this));
        getCommand("backupcode").setExecutor(new BackupCodeCommand(this));
        getCommand("setup2fa").setExecutor(new Setup2FACommand(this));
        getCommand("lockdown").setExecutor(new EmergencyLockdownCommand(this));
        getCommand("authstats").setExecutor(new AuthStatsCommand(this));
        
        getServer().getPluginManager().registerEvents(authListener, this);
        getServer().getPluginManager().registerEvents(securityListener, this);
        
        this.authLogger = new AuthLogger(this);
        this.emergencyLockdown = new EmergencyLockdown(this);
        this.authAnalytics = new AuthAnalytics(this);
        
        getServer().getScheduler().runTaskTimerAsynchronously(this, 
            () -> authLogger.cleanupOldLogs(), 
            20 * 3600, // 1 saat del
            20 * 3600  // 1 saat int
        );
        
        this.inventoryManager = new InventoryManager(this);
        this.inventoryManager.init();
        
        this.sessionManager = new SessionManager(this);
        
        Logger.info("Auth plugin initialized successfully!");
    }
    
    @Override
    public void onDisable() {
        getManagerFactory().getDatabaseManager().disconnect();
        
        getServer().getOnlinePlayers().forEach(player -> {
            player.kickPlayer("§cServer is restarting, please reconnect!");
        });
        
        RateLimiter.clearAll();
        WebServer.shutdown();
        CaptchaManager.cleanup();
    }
    
    public static Auth getInstance() {
        return instance;
    }
    
    public ManagerFactory getManagerFactory() {
        return managerFactory;
    }
    
    public SecurityListener getSecurityListener() {
        return securityListener;
    }
    
    public AuthListener getAuthListener() {
        return authListener;
    }
    
    public EmergencyLockdown getEmergencyLockdown() {
        return emergencyLockdown;
    }
    
    public AuthLogger getAuthLogger() {
        return authLogger;
    }
    
    public AuthAnalytics getAuthAnalytics() {
        return authAnalytics;
    }
    
    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }
    
    public SessionManager getSessionManager() {
        return sessionManager;
    }
} 