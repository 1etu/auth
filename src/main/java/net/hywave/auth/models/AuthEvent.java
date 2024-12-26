package net.hywave.auth.models;

import java.util.UUID;

public class AuthEvent {
    private final UUID playerUUID;
    private final AuthEventType type;
    private final String ipAddress;
    private final boolean success;
    private final String details;
    private final String geoLocation;
    private final long timestamp;

    public AuthEvent(UUID playerUUID, AuthEventType type, String ipAddress, 
                    boolean success, String details, String geoLocation, long timestamp) {
        this.playerUUID = playerUUID;
        this.type = type;
        this.ipAddress = ipAddress;
        this.success = success;
        this.details = details;
        this.geoLocation = geoLocation;
        this.timestamp = timestamp;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public AuthEventType getType() { return type; }
    public String getIpAddress() { return ipAddress; }
    public boolean isSuccess() { return success; }
    public String getDetails() { return details; }
    public String getGeoLocation() { return geoLocation; }
    public long getTimestamp() { return timestamp; }
} 