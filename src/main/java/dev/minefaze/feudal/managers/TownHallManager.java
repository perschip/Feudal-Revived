package dev.minefaze.feudal.managers;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Manages town hall operations, upgrades, and building placement
 */
public class TownHallManager {
    
    private final Feudal plugin;
    private final Map<UUID, BukkitRunnable> upgradeTimers;
    
    public TownHallManager(Feudal plugin) {
        this.plugin = plugin;
        this.upgradeTimers = new HashMap<>();
        
        // Start periodic update task
        startPeriodicUpdates();
    }
    
    /**
     * Start periodic updates for town halls and nexuses
     */
    private void startPeriodicUpdates() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateAllTownHalls();
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
    }
    
    /**
     * Update all town halls and nexuses
     */
    private void updateAllTownHalls() {
        for (Kingdom kingdom : plugin.getKingdomManager().getAllKingdoms()) {
            TownHall townHall = kingdom.getTownHall();
            Nexus nexus = kingdom.getNexus();
            
            if (townHall != null) {
                // Check for completed upgrades
                if (townHall.completeUpgradeIfReady()) {
                    onTownHallUpgradeComplete(kingdom);
                }
            }
            
            if (nexus != null) {
                // Process nexus regeneration
                nexus.startRegeneration();
                nexus.processRegeneration();
            }
        }
    }
    
    /**
     * Attempt to upgrade a town hall
     */
    public boolean upgradeTownHall(Kingdom kingdom, Player player) {
        TownHall townHall = kingdom.getTownHall();
        
        if (townHall == null) {
            plugin.getMessageManager().sendMessage(player, "townhall.not-found");
            return false;
        }
        
        if (!townHall.canUpgrade()) {
            if (townHall.getLevel() >= 10) {
                plugin.getMessageManager().sendMessage(player, "townhall.max-level");
            } else {
                plugin.getMessageManager().sendMessage(player, "townhall.already-upgrading");
            }
            return false;
        }
        
        int cost = townHall.getUpgradeCost();
        if (kingdom.getTreasury() < cost) {
            plugin.getMessageManager().sendMessage(player, "townhall.insufficient-funds", cost);
            return false;
        }
        
        // Deduct cost and start upgrade
        kingdom.setTreasury(kingdom.getTreasury() - cost);
        townHall.startUpgrade();
        
        // Notify kingdom members
        notifyKingdomMembers(kingdom, "townhall.upgrade-started", townHall.getLevel() + 1);
        
        // Save data
        plugin.getDataManager().saveKingdomData(kingdom);
        
        return true;
    }
    
    /**
     * Handle town hall upgrade completion
     */
    private void onTownHallUpgradeComplete(Kingdom kingdom) {
        TownHall townHall = kingdom.getTownHall();
        
        // Update nexus for new town hall level
        if (kingdom.getNexus() != null) {
            kingdom.getNexus().updateForTownHallLevel(townHall.getLevel());
        }
        
        // Upgrade the physical structure
        upgradeTownHallStructure(townHall);
        
        // Notify kingdom members
        notifyKingdomMembers(kingdom, "townhall.upgrade-complete", townHall.getLevel());
        
        // Save data
        plugin.getDataManager().saveKingdomData(kingdom);
    }
    
    /**
     * Build initial town hall structure using schematics
     */
    public void buildTownHallStructure(TownHall townHall) {
        Location center = townHall.getLocation();
        
        // Use SchematicManager to build the structure
        plugin.getSchematicManager().buildTownHall(townHall, center);
        
        // Also build the nexus structure if enabled
        if (plugin.getConfig().getBoolean("townhall.build-nexus", true)) {
            int nexusDistance = plugin.getConfig().getInt("townhall.nexus-distance", 10);
            plugin.getSchematicManager().buildNexus(townHall.getType(), center.clone().add(0, 0, nexusDistance));
        }
    }
    
    /**
     * Upgrade existing town hall structure using schematics
     */
    private void upgradeTownHallStructure(TownHall townHall) {
        Location center = townHall.getLocation();
        
        // Use SchematicManager to build the upgraded structure
        plugin.getSchematicManager().buildTownHall(townHall, center);
        
        plugin.getLogger().info("Upgraded town hall to level " + townHall.getLevel() + 
                               " for kingdom at " + center.toString());
    }
    
    /**
     * Build medieval castle structure
     */
    private void buildMedievalStructure(Location center, Material material, int level) {
        int size = Math.min(2 + level, 5); // Max size 5x5
        
        // Build walls
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                if (Math.abs(x) == size || Math.abs(z) == size) {
                    // Wall blocks
                    for (int y = 0; y <= 2 + level; y++) {
                        Block block = center.clone().add(x, y, z).getBlock();
                        block.setType(material);
                    }
                }
            }
        }
        
        // Build towers at corners
        int[] corners = {-size, size};
        for (int x : corners) {
            for (int z : corners) {
                for (int y = 0; y <= 4 + level; y++) {
                    Block block = center.clone().add(x, y, z).getBlock();
                    block.setType(material);
                }
            }
        }
    }
    
    /**
     * Build fantasy tower structure
     */
    private void buildFantasyStructure(Location center, Material material, int level) {
        int height = 3 + level * 2;
        
        // Build spiral tower
        for (int y = 0; y <= height; y++) {
            int radius = Math.max(1, 3 - y / 3);
            
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) {
                        Block block = center.clone().add(x, y, z).getBlock();
                        if (x * x + z * z == radius * radius || y == 0) {
                            block.setType(material);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Build modern fortress structure
     */
    private void buildModernStructure(Location center, Material material, int level) {
        int size = 2 + level;
        
        // Build rectangular fortress
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                for (int y = 0; y <= 2 + level; y++) {
                    if (Math.abs(x) == size || Math.abs(z) == size || y == 0 || y == 2 + level) {
                        Block block = center.clone().add(x, y, z).getBlock();
                        block.setType(material);
                    }
                }
            }
        }
    }
    
    /**
     * Notify all kingdom members of an event
     */
    private void notifyKingdomMembers(Kingdom kingdom, String messageKey, Object... args) {
        for (UUID memberId : kingdom.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                plugin.getMessageManager().sendMessage(member, messageKey, args);
            }
        }
    }
    
    /**
     * Get town hall at location
     */
    public TownHall getTownHallAt(Location location) {
        for (Kingdom kingdom : plugin.getKingdomManager().getAllKingdoms()) {
            TownHall townHall = kingdom.getTownHall();
            if (townHall != null && townHall.getLocation().distance(location) <= 10) {
                return townHall;
            }
        }
        return null;
    }
    
    /**
     * Check if location is within town hall area
     */
    public boolean isInTownHallArea(Location location) {
        return getTownHallAt(location) != null;
    }
    
    /**
     * Shutdown manager
     */
    public void shutdown() {
        // Cancel all upgrade timers
        for (BukkitRunnable timer : upgradeTimers.values()) {
            timer.cancel();
        }
        upgradeTimers.clear();
    }
}
