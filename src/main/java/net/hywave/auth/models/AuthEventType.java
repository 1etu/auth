package net.hywave.auth.models;

public enum AuthEventType {
    LOGIN_ATTEMPT,
    REGISTRATION,
    PASSWORD_CHANGE,
    BACKUP_CODE_USED,
    TWO_FACTOR_ATTEMPT,
    ACCOUNT_LOCKOUT,
    EMERGENCY_ACCESS,
    SUSPICIOUS_IP,
    FORCED_DISCONNECT,
    EMERGENCY_LOCKDOWN,
    SUSPICIOUS_SESSION,
    SESSION_CREATED,
    SESSION_INVALIDATED,
    SESSION_VALIDATION_FAILED
} 