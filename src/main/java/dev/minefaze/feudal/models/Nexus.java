package dev.minefaze.feudal.models;

import org.bukkit.Location;
import java.util.*;

/**
 * Represents a kingdom's nexus - the core health and power system
 */
public class Nexus {
    
    private UUID kingdomId;
    private int maxHealth;
    private int currentHealth;
    private int shieldPoints;
    private long lastDamageTime;
    private boolean isRegenerating;
    private Map<String, Integer> defenseStats;
    private List<String> activeEffects;
    private NexusStatus status;
    private Location location;
    
    public Nexus(UUID kingdomId, int townHallLevel) {
        this.kingdomId = kingdomId;
        this.maxHealth = calculateMaxHealth(townHallLevel);
        this.currentHealth = maxHealth;
        this.shieldPoints = 0;
        this.lastDamageTime = 0;
        this.isRegenerating = false;
        this.defenseStats = new HashMap<>();
        this.activeEffects = new ArrayList<>();
        this.status = NexusStatus.HEALTHY;
        
        initializeDefenseStats(townHallLevel);
    }
    
    // Getters and Setters
    public UUID getKingdomId() { return kingdomId; }
    public void setKingdomId(UUID kingdomId) { this.kingdomId = kingdomId; }
    
    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }
    
    public int getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(int currentHealth) { 
        this.currentHealth = Math.max(0, Math.min(currentHealth, maxHealth));
        updateStatus();
    }
    
    public int getShieldPoints() { return shieldPoints; }
    public void setShieldPoints(int shieldPoints) { this.shieldPoints = Math.max(0, shieldPoints); }
    
    public long getLastDamageTime() { return lastDamageTime; }
    public void setLastDamageTime(long lastDamageTime) { this.lastDamageTime = lastDamageTime; }
    
    public boolean isRegenerating() { return isRegenerating; }
    public void setRegenerating(boolean regenerating) { isRegenerating = regenerating; }
    
    public Map<String, Integer> getDefenseStats() { return defenseStats; }
    public void setDefenseStats(Map<String, Integer> defenseStats) { this.defenseStats = defenseStats; }
    
    public List<String> getActiveEffects() { return activeEffects; }
    public void setActiveEffects(List<String> activeEffects) { this.activeEffects = activeEffects; }
    
    public NexusStatus getStatus() { return status; }
    public void setStatus(NexusStatus status) { this.status = status; }
    
    /**
     * Calculate max health based on town hall level
     */
    private int calculateMaxHealth(int townHallLevel) {
        return switch (townHallLevel) {
            case 1 -> 1000;
            case 2 -> 1500;
            case 3 -> 2250;
            case 4 -> 3375;
            case 5 -> 5062;
            case 6 -> 7593;
            case 7 -> 11390;
            case 8 -> 17085;
            case 9 -> 25627;
            case 10 -> 38441;
            default -> 1000;
        };
    }
    
    /**
     * Initialize defense statistics based on town hall level
     */
    private void initializeDefenseStats(int townHallLevel) {
        defenseStats.put("armor", townHallLevel * 10);
        defenseStats.put("magic_resistance", townHallLevel * 8);
        defenseStats.put("regeneration_rate", townHallLevel * 2);
        defenseStats.put("shield_capacity", townHallLevel * 50);
    }
    
    /**
     * Update nexus status based on current health
     */
    private void updateStatus() {
        double healthPercentage = (double) currentHealth / maxHealth;
        
        if (healthPercentage >= 0.8) {
            status = NexusStatus.HEALTHY;
        } else if (healthPercentage >= 0.5) {
            status = NexusStatus.DAMAGED;
        } else if (healthPercentage >= 0.2) {
            status = NexusStatus.CRITICAL;
        } else if (healthPercentage > 0) {
            status = NexusStatus.NEARLY_DESTROYED;
        } else {
            status = NexusStatus.DESTROYED;
        }
    }
    
    /**
     * Deal damage to the nexus
     */
    public int takeDamage(int damage, DamageType damageType) {
        int actualDamage = calculateDamage(damage, damageType);
        
        // Apply to shield first
        if (shieldPoints > 0) {
            int shieldDamage = Math.min(actualDamage, shieldPoints);
            shieldPoints -= shieldDamage;
            actualDamage -= shieldDamage;
        }
        
        // Apply remaining damage to health
        if (actualDamage > 0) {
            currentHealth = Math.max(0, currentHealth - actualDamage);
            lastDamageTime = System.currentTimeMillis();
            isRegenerating = false;
            updateStatus();
        }
        
        return damage - actualDamage; // Return absorbed damage
    }
    
    /**
     * Calculate actual damage after defenses
     */
    private int calculateDamage(int baseDamage, DamageType damageType) {
        int defense = switch (damageType) {
            case PHYSICAL -> defenseStats.getOrDefault("armor", 0);
            case MAGICAL -> defenseStats.getOrDefault("magic_resistance", 0);
            case SIEGE -> defenseStats.getOrDefault("armor", 0) / 2; // Siege ignores half armor
            case TRUE -> 0; // True damage ignores all defenses
        };
        
        // Calculate damage reduction (max 75% reduction)
        double reduction = Math.min(0.75, defense / (defense + 100.0));
        return (int) (baseDamage * (1 - reduction));
    }
    
    /**
     * Heal the nexus
     */
    public void heal(int amount) {
        currentHealth = Math.min(maxHealth, currentHealth + amount);
        updateStatus();
    }
    
    /**
     * Add shield points
     */
    public void addShield(int amount) {
        int maxShield = defenseStats.getOrDefault("shield_capacity", 0);
        shieldPoints = Math.min(maxShield, shieldPoints + amount);
    }
    
    /**
     * Start natural regeneration if conditions are met
     */
    public void startRegeneration() {
        long timeSinceLastDamage = System.currentTimeMillis() - lastDamageTime;
        if (timeSinceLastDamage >= 300000 && currentHealth < maxHealth) { // 5 minutes
            isRegenerating = true;
        }
    }
    
    /**
     * Process regeneration tick
     */
    public void processRegeneration() {
        if (isRegenerating && currentHealth < maxHealth) {
            int regenRate = defenseStats.getOrDefault("regeneration_rate", 2);
            heal(regenRate);
        }
    }
    
    /**
     * Get health percentage
     */
    public double getHealthPercentage() {
        return (double) currentHealth / maxHealth;
    }
    
    /**
     * Check if nexus is destroyed
     */
    public boolean isDestroyed() {
        return currentHealth <= 0;
    }
    
    /**
     * Get display color based on health
     */
    public String getHealthColor() {
        return switch (status) {
            case HEALTHY -> "§a";
            case DAMAGED -> "§e";
            case CRITICAL -> "§6";
            case NEARLY_DESTROYED -> "§c";
            case DESTROYED -> "§4";
        };
    }
    
    /**
     * Update nexus for town hall level change
     */
    public void updateForTownHallLevel(int newLevel) {
        int oldMaxHealth = maxHealth;
        maxHealth = calculateMaxHealth(newLevel);
        
        // Scale current health proportionally
        if (oldMaxHealth > 0) {
            double healthRatio = (double) currentHealth / oldMaxHealth;
            currentHealth = (int) (maxHealth * healthRatio);
        } else {
            currentHealth = maxHealth;
        }
        
        initializeDefenseStats(newLevel);
        updateStatus();
    }
    
    // Location methods
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    
    /**
     * Add temporary effect
     */
    public void addEffect(String effect) {
        if (!activeEffects.contains(effect)) {
            activeEffects.add(effect);
        }
    }
    
    /**
     * Remove effect
     */
    public void removeEffect(String effect) {
        activeEffects.remove(effect);
    }
    
    /**
     * Check if effect is active
     */
    public boolean hasEffect(String effect) {
        return activeEffects.contains(effect);
    }
    
    /**
     * Get armor value from defense stats
     */
    public int getArmor() {
        return defenseStats.getOrDefault("armor", 0);
    }
    
    /**
     * Get magic resistance value from defense stats
     */
    public int getMagicResistance() {
        return defenseStats.getOrDefault("magic_resistance", 0);
    }
    
    /**
     * Get regeneration rate from defense stats
     */
    public int getRegenerationRate() {
        return defenseStats.getOrDefault("regeneration_rate", 2);
    }
    
    /**
     * Get shield capacity from defense stats
     */
    public int getShieldCapacity() {
        return defenseStats.getOrDefault("shield_capacity", 0);
    }
    
    /**
     * Nexus health status levels
     */
    public enum NexusStatus {
        HEALTHY("Healthy", "§a■■■■■"),
        DAMAGED("Damaged", "§e■■■■§8■"),
        CRITICAL("Critical", "§6■■■§8■■"),
        NEARLY_DESTROYED("Nearly Destroyed", "§c■■§8■■■"),
        DESTROYED("Destroyed", "§4■§8■■■■");
        
        private final String displayName;
        private final String healthBar;
        
        NexusStatus(String displayName, String healthBar) {
            this.displayName = displayName;
            this.healthBar = healthBar;
        }
        
        public String getDisplayName() { return displayName; }
        public String getHealthBar() { return healthBar; }
    }
    
    /**
     * Types of damage that can be dealt to nexus
     */
    public enum DamageType {
        PHYSICAL,   // Reduced by armor
        MAGICAL,    // Reduced by magic resistance
        SIEGE,      // Ignores half armor
        TRUE        // Ignores all defenses
    }
}
