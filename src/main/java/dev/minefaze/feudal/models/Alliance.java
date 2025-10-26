package dev.minefaze.feudal.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Alliance {
    
    private final UUID allianceId;
    private final UUID kingdom1Id;
    private final UUID kingdom2Id;
    private final LocalDateTime createdAt;
    private AllianceType type;
    private boolean active;
    
    public Alliance(UUID kingdom1Id, UUID kingdom2Id, AllianceType type) {
        this.allianceId = UUID.randomUUID();
        this.kingdom1Id = kingdom1Id;
        this.kingdom2Id = kingdom2Id;
        this.type = type;
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }
    
    // Getters and Setters
    public UUID getAllianceId() { return allianceId; }
    public UUID getKingdom1Id() { return kingdom1Id; }
    public UUID getKingdom2Id() { return kingdom2Id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public AllianceType getType() { return type; }
    public void setType(AllianceType type) { this.type = type; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    // Utility methods
    public boolean involvesKingdom(UUID kingdomId) {
        return kingdom1Id.equals(kingdomId) || kingdom2Id.equals(kingdomId);
    }
    
    public UUID getOtherKingdom(UUID kingdomId) {
        if (kingdom1Id.equals(kingdomId)) return kingdom2Id;
        if (kingdom2Id.equals(kingdomId)) return kingdom1Id;
        return null;
    }
    
    public boolean isAlly() {
        return type == AllianceType.ALLY && active;
    }
    
    public boolean isEnemy() {
        return type == AllianceType.ENEMY && active;
    }
    
    public enum AllianceType {
        ALLY("Alliance", "§a"),
        ENEMY("War", "§c"),
        NEUTRAL("Neutral", "§7");
        
        private final String displayName;
        private final String color;
        
        AllianceType(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
        public String getColoredName() { return color + displayName; }
    }
}
