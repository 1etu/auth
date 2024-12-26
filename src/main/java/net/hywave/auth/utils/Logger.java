package net.hywave.auth.utils;

import net.hywave.auth.Auth;
import org.bukkit.Bukkit;

public class Logger {
    private static Auth plugin;
    
    public static void init(Auth instance) {
        plugin = instance;
    }
    
    public static void error(String message, Throwable e) {
        Bukkit.getConsoleSender().sendMessage("§c" + message);
        Bukkit.getConsoleSender().sendMessage("§cCause: " + e.getMessage());
        Bukkit.getConsoleSender().sendMessage("§cStacktrace:");
        for (StackTraceElement element : e.getStackTrace()) {
            Bukkit.getConsoleSender().sendMessage("§c  at " + element.toString());
        }
        
        if (e.getCause() != null) {
            Bukkit.getConsoleSender().sendMessage("§cCaused by: " + e.getCause().getMessage());
            for (StackTraceElement element : e.getCause().getStackTrace()) {
                Bukkit.getConsoleSender().sendMessage("§c  at " + element.toString());
            }
        }
    }
    
    public static void warn(String message) {
        Bukkit.getConsoleSender().sendMessage("§e" + message);
    }
    
    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage("§7" + message);
    }
    
    public static void debug(String message) {
        if (plugin.getConfig().getBoolean("settings.debug", false)) {
            Bukkit.getConsoleSender().sendMessage("§b[DEBUG] " + message);
        }
    }
} 