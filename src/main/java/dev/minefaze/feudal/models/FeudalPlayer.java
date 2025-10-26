package dev.minefaze.feudal.models;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FeudalPlayer {
    
    private final UUID playerId;
    private String playerName;
    private Kingdom kingdom;
    private Map<Profession, Integer> professionLevels;
    private Map<Attribute, Integer> attributes;
    private int totalExperience;
    private boolean inCombat;
    private Challenge activeChallenge;
    
    public FeudalPlayer(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.professionLevels = new HashMap<>();
        this.attributes = new HashMap<>();
        this.totalExperience = 0;
        this.inCombat = false;
        
        // Initialize default profession levels
        for (Profession profession : Profession.values()) {
            professionLevels.put(profession, 1);
        }
        
        // Initialize default attributes
        for (Attribute attribute : Attribute.values()) {
            attributes.put(attribute, 10);
        }
    }
    
    // Getters and Setters
    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    
    public Kingdom getKingdom() { return kingdom; }
    public void setKingdom(Kingdom kingdom) { this.kingdom = kingdom; }
    
    public Map<Profession, Integer> getProfessionLevels() { return professionLevels; }
    public int getProfessionLevel(Profession profession) { 
        return professionLevels.getOrDefault(profession, 1); 
    }
    public void setProfessionLevel(Profession profession, int level) { 
        professionLevels.put(profession, level); 
    }
    
    public Map<Attribute, Integer> getAttributes() { return attributes; }
    public int getAttribute(Attribute attribute) { 
        return attributes.getOrDefault(attribute, 10); 
    }
    public void setAttribute(Attribute attribute, int value) { 
        attributes.put(attribute, value); 
    }
    
    public int getTotalExperience() { return totalExperience; }
    public void setTotalExperience(int totalExperience) { this.totalExperience = totalExperience; }
    public void addExperience(int experience) { this.totalExperience += experience; }
    
    public boolean isInCombat() { return inCombat; }
    public void setInCombat(boolean inCombat) { this.inCombat = inCombat; }
    
    public Challenge getActiveChallenge() { return activeChallenge; }
    public void setActiveChallenge(Challenge activeChallenge) { this.activeChallenge = activeChallenge; }
    
    // Utility methods
    public boolean hasKingdom() { return kingdom != null; }
    public boolean isKingdomLeader() { 
        return hasKingdom() && kingdom.getLeader().equals(playerId); 
    }
    
    public int getCombatPower() {
        int basePower = getAttribute(Attribute.STRENGTH) + getAttribute(Attribute.DEFENSE);
        int professionBonus = getProfessionLevel(Profession.WARRIOR) * 2;
        return basePower + professionBonus;
    }
    
    public boolean canChallenge() {
        return !inCombat && activeChallenge == null;
    }
    
    // Additional methods for GUI system
    public int getTotalProfessionLevel() {
        return professionLevels.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    public int getProfessionExperience(Profession profession) {
        // For now, return a calculated value based on level
        // This should be replaced with actual stored experience per profession
        int level = getProfessionLevel(profession);
        return level * 100; // Simple calculation for demo
    }
    
    public int getAvailableAttributePoints() {
        // Calculate available points based on total level minus spent points
        int earnedPoints = getTotalProfessionLevel() / 5; // 1 point per 5 total levels
        int spentPoints = attributes.values().stream().mapToInt(v -> v - 10).sum(); // Subtract base 10
        return Math.max(0, earnedPoints - spentPoints);
    }
}
