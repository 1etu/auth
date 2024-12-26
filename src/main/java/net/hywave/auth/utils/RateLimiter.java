package net.hywave.auth.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class RateLimiter {
    private static final Map<String, Integer> ipAttempts = new ConcurrentHashMap<>();
    private static final Map<String, Long> ipCooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> accountAttempts = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> accountLocks = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> captchaRequired = new ConcurrentHashMap<>();
    
    @SuppressWarnings("unused")
    private static final int MAX_IP_ATTEMPTS = 10;
    private static final int MAX_ACCOUNT_ATTEMPTS = 5;
    @SuppressWarnings("unused")
    private static final long IP_COOLDOWN_DURATION = 300000; // 5 dk
    private static final long ACCOUNT_LOCK_DURATION = 1800000; // 30 dk
    private static final long BLOCK_DURATION = 900000; // 15 dk
    private static final int CAPTCHA_THRESHOLD = 2;
    
    public static boolean isIpBlocked(String ip) {
        Long cooldownUntil = ipCooldowns.get(ip);
        if (cooldownUntil != null && cooldownUntil > System.currentTimeMillis()) {
            return true;
        }
        ipCooldowns.remove(ip);
        return false;
    }
    
    public static boolean isAccountLocked(UUID uuid) {
        Long lockedUntil = accountLocks.get(uuid);
        if (lockedUntil != null && lockedUntil > System.currentTimeMillis()) {
            return true;
        }
        accountLocks.remove(uuid);
        accountAttempts.remove(uuid);
        return false;
    }
    
    public static boolean isCaptchaRequired(String ip) {
        return captchaRequired.getOrDefault(ip, false);
    }
    
    public static void recordFailedAttempt(String ip, UUID uuid) {
        ipAttempts.merge(ip, 1, Integer::sum);
        accountAttempts.merge(uuid, 1, Integer::sum);
        
        int attempts = ipAttempts.get(ip);
        if (attempts == CAPTCHA_THRESHOLD) {
            captchaRequired.put(ip, true);
        } else if (attempts > CAPTCHA_THRESHOLD) {
            ipCooldowns.put(ip, System.currentTimeMillis() + BLOCK_DURATION);
            ipAttempts.remove(ip);
            captchaRequired.remove(ip);
        }
        
        if (accountAttempts.get(uuid) >= MAX_ACCOUNT_ATTEMPTS) {
            accountLocks.put(uuid, System.currentTimeMillis() + ACCOUNT_LOCK_DURATION);
            accountAttempts.remove(uuid);
        }
    }
    
    public static void clearAttempts(String ip, UUID uuid) {
        ipAttempts.remove(ip);
        accountAttempts.remove(uuid);
        captchaRequired.remove(ip);
    }
    
    public static long getRemainingCooldown(String ip) {
        Long cooldownUntil = ipCooldowns.get(ip);
        return cooldownUntil != null ? Math.max(0, cooldownUntil - System.currentTimeMillis()) : 0;
    }
    
    public static long getRemainingLockTime(UUID uuid) {
        Long lockedUntil = accountLocks.get(uuid);
        return lockedUntil != null ? Math.max(0, lockedUntil - System.currentTimeMillis()) : 0;
    }
    
    public static void clearAll() {
        ipAttempts.clear();
        ipCooldowns.clear();
        accountAttempts.clear();
        accountLocks.clear();
    }
    
    @SuppressWarnings("unused")
    private static void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        ipCooldowns.entrySet().removeIf(entry -> entry.getValue() < now);
        accountLocks.entrySet().removeIf(entry -> entry.getValue() < now);
    }
} 