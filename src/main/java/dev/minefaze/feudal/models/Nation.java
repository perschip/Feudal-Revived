package dev.minefaze.feudal.models;

import java.time.LocalDateTime;
import java.util.*;

public class Nation {
    
    private final UUID nationId;
    private String name;
    private String description;
    private UUID leaderKingdomId;
    private final Set<UUID> memberKingdomIds;
    private final LocalDateTime createdAt;
    private int treasury;
    private boolean active;
    
    public Nation(String name, UUID leaderKingdomId) {
        this.nationId = UUID.randomUUID();
        this.name = name;
        this.leaderKingdomId = leaderKingdomId;
        this.memberKingdomIds = new HashSet<>();
        this.memberKingdomIds.add(leaderKingdomId);
        this.createdAt = LocalDateTime.now();
        this.treasury = 0;
        this.active = true;
        this.description = "A mighty nation led by " + name;
    }
    
    // Getters and Setters
    public UUID getNationId() { return nationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getLeaderKingdomId() { return leaderKingdomId; }
    public void setLeaderKingdomId(UUID leaderKingdomId) { this.leaderKingdomId = leaderKingdomId; }
    public Set<UUID> getMemberKingdomIds() { return new HashSet<>(memberKingdomIds); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public int getTreasury() { return treasury; }
    public void setTreasury(int treasury) { this.treasury = treasury; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    // Member management
    public boolean addKingdom(UUID kingdomId) {
        return memberKingdomIds.add(kingdomId);
    }
    
    public boolean removeKingdom(UUID kingdomId) {
        if (kingdomId.equals(leaderKingdomId)) {
            return false; // Cannot remove leader kingdom
        }
        return memberKingdomIds.remove(kingdomId);
    }
    
    public boolean isMember(UUID kingdomId) {
        return memberKingdomIds.contains(kingdomId);
    }
    
    public boolean isLeader(UUID kingdomId) {
        return leaderKingdomId.equals(kingdomId);
    }
    
    public int getMemberCount() {
        return memberKingdomIds.size();
    }
    
    // Treasury management
    public void addToTreasury(int amount) {
        this.treasury += amount;
    }
    
    public boolean withdrawFromTreasury(int amount) {
        if (treasury >= amount) {
            treasury -= amount;
            return true;
        }
        return false;
    }
    
    // Power calculation
    public int getTotalPower() {
        // This would be calculated based on member kingdoms' power
        return getMemberCount() * 100; // Placeholder calculation
    }
    
    // Utility methods
    public List<UUID> getAllies() {
        return new ArrayList<>(memberKingdomIds);
    }
    
    public boolean canDeclareWar() {
        return active && getMemberCount() >= 2; // Require at least 2 kingdoms
    }
}
