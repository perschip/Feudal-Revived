package dev.minefaze.feudal.models;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.*;

/**
 * Represents a kingdom's town hall with levels and capabilities
 */
public class TownHall {
    
    private UUID kingdomId;
    private Location location;
    private int level;
    private TownHallType type;
    private long constructionTime;
    private boolean isUpgrading;
    private long upgradeStartTime;
    private Map<String, Object> customData;
    
    public TownHall(UUID kingdomId, Location location, TownHallType type) {
        this.kingdomId = kingdomId;
        this.location = location;
        this.level = 1;
        this.type = type;
        this.constructionTime = System.currentTimeMillis();
        this.isUpgrading = false;
        this.upgradeStartTime = 0;
        this.customData = new HashMap<>();
    }
    
    // Getters and Setters
    public UUID getKingdomId() { return kingdomId; }
    public void setKingdomId(UUID kingdomId) { this.kingdomId = kingdomId; }
    
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    
    public TownHallType getType() { return type; }
    public void setType(TownHallType type) { this.type = type; }
    
    public long getConstructionTime() { return constructionTime; }
    public void setConstructionTime(long constructionTime) { this.constructionTime = constructionTime; }
    
    public boolean isUpgrading() { return isUpgrading; }
    public void setUpgrading(boolean upgrading) { isUpgrading = upgrading; }
    
    public long getUpgradeStartTime() { return upgradeStartTime; }
    public void setUpgradeStartTime(long upgradeStartTime) { this.upgradeStartTime = upgradeStartTime; }
    
    public Map<String, Object> getCustomData() { return customData; }
    public void setCustomData(Map<String, Object> customData) { this.customData = customData; }
    
    /**
     * Get the maximum number of territories this town hall level allows
     */
    public int getMaxTerritories() {
        return switch (level) {
            case 1 -> 9;   // 3x3 initial
            case 2 -> 16;  // 4x4
            case 3 -> 25;  // 5x5
            case 4 -> 36;  // 6x6
            case 5 -> 49;  // 7x7
            case 6 -> 64;  // 8x8
            case 7 -> 81;  // 9x9
            case 8 -> 100; // 10x10
            case 9 -> 121; // 11x11
            case 10 -> 144; // 12x12 (max level)
            default -> 9;
        };
    }
    
    /**
     * Get the maximum number of members this town hall level allows
     */
    public int getMaxMembers() {
        return switch (level) {
            case 1 -> 5;
            case 2 -> 8;
            case 3 -> 12;
            case 4 -> 16;
            case 5 -> 20;
            case 6 -> 25;
            case 7 -> 30;
            case 8 -> 35;
            case 9 -> 40;
            case 10 -> 50;
            default -> 5;
        };
    }
    
    /**
     * Get the number of defense slots available
     */
    public int getDefenseSlots() {
        return switch (level) {
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 4;
            case 4 -> 6;
            case 5 -> 8;
            case 6 -> 10;
            case 7 -> 12;
            case 8 -> 15;
            case 9 -> 18;
            case 10 -> 25;
            default -> 2;
        };
    }
    
    /**
     * Get the number of army camps available
     */
    public int getArmyCamps() {
        return switch (level) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 2;
            case 4 -> 3;
            case 5 -> 3;
            case 6 -> 4;
            case 7 -> 4;
            case 8 -> 5;
            case 9 -> 5;
            case 10 -> 6;
            default -> 1;
        };
    }
    
    /**
     * Check if town hall can be upgraded
     */
    public boolean canUpgrade() {
        return level < 10 && !isUpgrading;
    }
    
    /**
     * Get upgrade cost for next level
     */
    public int getUpgradeCost() {
        return switch (level) {
            case 1 -> 1000;
            case 2 -> 2500;
            case 3 -> 5000;
            case 4 -> 10000;
            case 5 -> 20000;
            case 6 -> 40000;
            case 7 -> 80000;
            case 8 -> 160000;
            case 9 -> 320000;
            default -> Integer.MAX_VALUE; // Max level
        };
    }
    
    /**
     * Get upgrade time in seconds for next level
     */
    public long getUpgradeTime() {
        return switch (level) {
            case 1 -> 300;    // 5 minutes
            case 2 -> 900;    // 15 minutes
            case 3 -> 1800;   // 30 minutes
            case 4 -> 3600;   // 1 hour
            case 5 -> 7200;   // 2 hours
            case 6 -> 14400;  // 4 hours
            case 7 -> 28800;  // 8 hours
            case 8 -> 57600;  // 16 hours
            case 9 -> 86400;  // 24 hours
            default -> 0;     // Max level
        };
    }
    
    /**
     * Start upgrading the town hall
     */
    public void startUpgrade() {
        if (canUpgrade()) {
            this.isUpgrading = true;
            this.upgradeStartTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Complete the upgrade if time has passed
     */
    public boolean completeUpgradeIfReady() {
        if (!isUpgrading) return false;
        
        long upgradeTimeMs = getUpgradeTime() * 1000;
        if (System.currentTimeMillis() - upgradeStartTime >= upgradeTimeMs) {
            this.level++;
            this.isUpgrading = false;
            this.upgradeStartTime = 0;
            return true;
        }
        return false;
    }
    
    /**
     * Get remaining upgrade time in seconds
     */
    public long getRemainingUpgradeTime() {
        if (!isUpgrading) return 0;
        
        long upgradeTimeMs = getUpgradeTime() * 1000;
        long elapsed = System.currentTimeMillis() - upgradeStartTime;
        long remaining = upgradeTimeMs - elapsed;
        
        return Math.max(0, remaining / 1000);
    }
    
    /**
     * Get the main material for this town hall type and level
     */
    public Material getMainMaterial() {
        return switch (type) {
            case MEDIEVAL -> switch (level) {
                case 1, 2 -> Material.COBBLESTONE;
                case 3, 4 -> Material.STONE_BRICKS;
                case 5, 6 -> Material.POLISHED_ANDESITE;
                case 7, 8 -> Material.QUARTZ_BLOCK;
                case 9, 10 -> Material.NETHERITE_BLOCK;
                default -> Material.COBBLESTONE;
            };
            case FANTASY -> switch (level) {
                case 1, 2 -> Material.OAK_PLANKS;
                case 3, 4 -> Material.SPRUCE_PLANKS;
                case 5, 6 -> Material.DARK_OAK_PLANKS;
                case 7, 8 -> Material.PURPUR_BLOCK;
                case 9, 10 -> Material.END_STONE_BRICKS;
                default -> Material.OAK_PLANKS;
            };
            case MODERN -> switch (level) {
                case 1, 2 -> Material.SMOOTH_STONE;
                case 3, 4 -> Material.WHITE_CONCRETE;
                case 5, 6 -> Material.LIGHT_GRAY_CONCRETE;
                case 7, 8 -> Material.IRON_BLOCK;
                case 9, 10 -> Material.DIAMOND_BLOCK;
                default -> Material.SMOOTH_STONE;
            };
        };
    }
    
    /**
     * Town Hall building styles
     */
    public enum TownHallType {
        MEDIEVAL("Medieval Castle", "A traditional stone castle with towers and walls"),
        FANTASY("Mystical Tower", "A magical tower with enchanted elements"),
        MODERN("Fortress Base", "A modern military-style compound");
        
        private final String displayName;
        private final String description;
        
        TownHallType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
}
