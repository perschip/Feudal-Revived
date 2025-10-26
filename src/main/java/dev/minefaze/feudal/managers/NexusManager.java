package dev.minefaze.feudal.managers;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Manages nexus placement, protection, and war-based damage mechanics
 */
public class NexusManager implements Listener {
    
    private final Feudal plugin;
    private final Map<Location, UUID> nexusLocations; // Location -> Kingdom ID
    private final Map<UUID, Location> kingdomNexus; // Kingdom ID -> Nexus Location
    private final Set<Location> activeNexus; // Currently active nexus locations
    
    public NexusManager(Feudal plugin) {
        this.plugin = plugin;
        this.nexusLocations = new HashMap<>();
        this.kingdomNexus = new HashMap<>();
        this.activeNexus = new HashSet<>();
        
        // Register event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Start nexus regeneration task
        startRegenerationTask();
    }
    
    /**
     * Place a nexus for a kingdom at the specified location
     */
    public boolean placeNexus(Kingdom kingdom, Location location, Player placer) {
        plugin.getLogger().info("NexusManager.placeNexus called for kingdom " + kingdom.getName() + " at " + location);
        
        // Check if kingdom already has a nexus
        if (kingdomNexus.containsKey(kingdom.getKingdomId())) {
            plugin.getLogger().info("Kingdom already has a nexus, placement failed");
            plugin.getMessageManager().sendMessage(placer, "nexus.already-exists");
            return false;
        }
        
        // Check if location is in kingdom territory
        Territory territory = plugin.getKingdomManager().getTerritoryAt(location);
        if (territory == null || !territory.getKingdomId().equals(kingdom.getKingdomId())) {
            plugin.getLogger().info("Location not in kingdom territory, placement failed. Territory: " + territory);
            plugin.getMessageManager().sendMessage(placer, "nexus.must-be-in-territory");
            return false;
        }
        
        // Check if location is clear
        if (!isLocationClear(location)) {
            plugin.getLogger().info("Location not clear for nexus placement");
            plugin.getMessageManager().sendMessage(placer, "nexus.location-blocked");
            return false;
        }
        
        plugin.getLogger().info("All checks passed, building nexus structure");
        // Place the end beacon structure
        buildNexusStructure(location, kingdom);
        
        // Register the nexus
        nexusLocations.put(location, kingdom.getKingdomId());
        kingdomNexus.put(kingdom.getKingdomId(), location);
        activeNexus.add(location);
        
        // Initialize nexus in kingdom
        if (kingdom.getNexus() == null) {
            int townHallLevel = kingdom.getTownHall() != null ? kingdom.getTownHall().getLevel() : 1;
            Nexus nexus = new Nexus(kingdom.getKingdomId(), townHallLevel);
            nexus.setLocation(location);
            kingdom.setNexus(nexus);
        } else {
            kingdom.getNexus().setLocation(location);
        }
        
        // Save data
        plugin.getDataManager().saveKingdomData(kingdom);
        
        // Notify kingdom members
        notifyKingdomMembers(kingdom, "nexus.placed", location.getBlockX(), location.getBlockZ());
        
        plugin.getLogger().info("Nexus placed for kingdom " + kingdom.getName() + 
                               " at " + location.getWorld().getName() + " " + 
                               location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        
        return true;
    }
    
    /**
     * Remove a nexus from a kingdom
     */
    public boolean removeNexus(Kingdom kingdom, Player remover) {
        Location nexusLoc = kingdomNexus.get(kingdom.getKingdomId());
        if (nexusLoc == null) {
            plugin.getMessageManager().sendMessage(remover, "nexus.not-found");
            return false;
        }
        
        // Remove the physical structure
        destroyNexusStructure(nexusLoc);
        
        // Unregister the nexus
        nexusLocations.remove(nexusLoc);
        kingdomNexus.remove(kingdom.getKingdomId());
        activeNexus.remove(nexusLoc);
        
        // Remove from kingdom
        kingdom.setNexus(null);
        
        // Save data
        plugin.getDataManager().saveKingdomData(kingdom);
        
        // Notify kingdom members
        notifyKingdomMembers(kingdom, "nexus.removed");
        
        return true;
    }
    
    /**
     * Build the physical nexus structure (end beacon)
     */
    private void buildNexusStructure(Location location, Kingdom kingdom) {
        plugin.getLogger().info("Building nexus structure at " + location);
        
        // Build the beacon base (3x3 iron blocks)
        plugin.getLogger().info("Placing iron block base...");
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block baseBlock = location.clone().add(x, -1, z).getBlock();
                plugin.getLogger().info("Setting iron block at " + baseBlock.getLocation() + " (was " + baseBlock.getType() + ")");
                baseBlock.setType(Material.IRON_BLOCK);
            }
        }
        
        // Place the end beacon (beacon block with special properties)
        plugin.getLogger().info("Placing beacon block...");
        Block nexusBlock = location.getBlock();
        plugin.getLogger().info("Setting beacon at " + nexusBlock.getLocation() + " (was " + nexusBlock.getType() + ")");
        nexusBlock.setType(Material.BEACON);
        
        // Add some decorative blocks around it
        plugin.getLogger().info("Placing decorative blocks...");
        Material decorMaterial = getDecorativeMaterial(kingdom.getTownHall());
        plugin.getLogger().info("Using decorative material: " + decorMaterial);
        
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    Block decorBlock = location.clone().add(x, 0, z).getBlock();
                    if (decorBlock.getType() == Material.AIR) {
                        plugin.getLogger().info("Setting decorative block at " + decorBlock.getLocation());
                        decorBlock.setType(decorMaterial);
                    } else {
                        plugin.getLogger().info("Skipping decorative block at " + decorBlock.getLocation() + " - not air: " + decorBlock.getType());
                    }
                }
            }
        }
        
        plugin.getLogger().info("Finished building nexus structure at " + location.toString());
    }
    
    /**
     * Destroy the physical nexus structure
     */
    private void destroyNexusStructure(Location location) {
        // Remove the beacon and base
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -1; y <= 1; y++) {
                    Block block = location.clone().add(x, y, z).getBlock();
                    if (block.getType() == Material.BEACON || 
                        block.getType() == Material.IRON_BLOCK ||
                        isDecorativeMaterial(block.getType())) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }
    
    /**
     * Get decorative material based on town hall type
     */
    private Material getDecorativeMaterial(TownHall townHall) {
        if (townHall == null) return Material.STONE_BRICKS;
        
        return switch (townHall.getType()) {
            case MEDIEVAL -> Material.STONE_BRICKS;
            case FANTASY -> Material.PURPUR_BLOCK;
            case MODERN -> Material.QUARTZ_BLOCK;
        };
    }
    
    /**
     * Check if material is decorative (used in nexus structure)
     */
    private boolean isDecorativeMaterial(Material material) {
        return material == Material.STONE_BRICKS || 
               material == Material.PURPUR_BLOCK || 
               material == Material.QUARTZ_BLOCK;
    }
    
    /**
     * Check if location is clear for nexus placement
     */
    private boolean isLocationClear(Location location) {
        plugin.getLogger().info("Checking if location is clear for nexus placement at " + location);
        
        // Simplified check - just ensure the center beacon location and base are suitable
        Block centerBlock = location.getBlock();
        Block baseBlock = location.clone().add(0, -1, 0).getBlock();
        
        // Check if we can place the beacon (center block should be air or replaceable)
        if (centerBlock.getType() != Material.AIR && !centerBlock.getType().isReplaceable()) {
            plugin.getLogger().info("Center block not suitable for beacon: " + centerBlock.getType());
            return false;
        }
        
        // Check if base has solid ground (at least the center)
        if (!baseBlock.getType().isSolid()) {
            plugin.getLogger().info("Base block not solid: " + baseBlock.getType());
            return false;
        }
        
        plugin.getLogger().info("Location is clear for nexus placement (simplified check)");
        return true;
    }
    
    /**
     * Damage a nexus during war
     */
    public boolean damageNexus(Location nexusLocation, int damage, Kingdom attackingKingdom) {
        UUID kingdomId = nexusLocations.get(nexusLocation);
        if (kingdomId == null) {
            return false;
        }
        
        Kingdom defendingKingdom = plugin.getKingdomManager().getKingdom(kingdomId);
        if (defendingKingdom == null || defendingKingdom.getNexus() == null) {
            return false;
        }
        
        // Check if kingdoms are at war
        if (!areKingdomsAtWar(attackingKingdom, defendingKingdom)) {
            return false;
        }
        
        Nexus nexus = defendingKingdom.getNexus();
        int remainingDamage = nexus.takeDamage(damage, Nexus.DamageType.PHYSICAL);
        
        // Check if nexus is destroyed
        if (nexus.getCurrentHealth() <= 0) {
            handleNexusDestruction(defendingKingdom, attackingKingdom);
        } else {
            // Notify kingdoms of damage
            notifyKingdomMembers(defendingKingdom, "nexus.damaged", 
                               nexus.getCurrentHealth(), nexus.getMaxHealth());
            notifyKingdomMembers(attackingKingdom, "nexus.damage-dealt", 
                               damage - remainingDamage, defendingKingdom.getName());
        }
        
        // Save data
        plugin.getDataManager().saveKingdomData(defendingKingdom);
        
        return true;
    }
    
    /**
     * Handle nexus destruction
     */
    private void handleNexusDestruction(Kingdom defendingKingdom, Kingdom attackingKingdom) {
        Location nexusLoc = kingdomNexus.get(defendingKingdom.getKingdomId());
        
        // Remove the physical structure
        destroyNexusStructure(nexusLoc);
        
        // Unregister the nexus
        nexusLocations.remove(nexusLoc);
        kingdomNexus.remove(defendingKingdom.getKingdomId());
        activeNexus.remove(nexusLoc);
        
        // Remove from kingdom
        defendingKingdom.setNexus(null);
        
        // Notify all players
        plugin.getServer().broadcastMessage("§c§l⚡ NEXUS DESTROYED! ⚡");
        plugin.getServer().broadcastMessage("§e" + attackingKingdom.getName() + 
                                           " §7has destroyed §c" + defendingKingdom.getName() + 
                                           "§7's nexus!");
        
        // End the war (nexus destruction ends the war)
        endWar(attackingKingdom, defendingKingdom);
        
        // Save data
        plugin.getDataManager().saveKingdomData(defendingKingdom);
        
        plugin.getLogger().info("Nexus of kingdom " + defendingKingdom.getName() + 
                               " destroyed by " + attackingKingdom.getName());
    }
    
    /**
     * Check if two kingdoms are at war
     */
    private boolean areKingdomsAtWar(Kingdom kingdom1, Kingdom kingdom2) {
        // This would integrate with your war system
        // For now, check if they have enemy alliance
        Alliance alliance = plugin.getAllianceManager().getAlliance(
            kingdom1.getKingdomId(), kingdom2.getKingdomId());
        
        return alliance != null && alliance.getType() == Alliance.AllianceType.ENEMY;
    }
    
    /**
     * End war between kingdoms
     */
    private void endWar(Kingdom winner, Kingdom loser) {
        // This would integrate with your war system
        // For now, just remove enemy alliance
        plugin.getAllianceManager().removeAlliance(winner.getKingdomId(), loser.getKingdomId());
    }
    
    /**
     * Start nexus regeneration task
     */
    private void startRegenerationTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfig().getBoolean("nexus.enable-regeneration", true)) {
                    return;
                }
                
                long currentTime = System.currentTimeMillis();
                int regenDelay = plugin.getConfig().getInt("nexus.regen-delay", 5) * 60 * 1000; // minutes to ms
                int regenRate = plugin.getConfig().getInt("nexus.regen-rate", 10);
                
                for (UUID kingdomId : kingdomNexus.keySet()) {
                    Kingdom kingdom = plugin.getKingdomManager().getKingdom(kingdomId);
                    if (kingdom != null && kingdom.getNexus() != null) {
                        Nexus nexus = kingdom.getNexus();
                        
                        // Check if enough time has passed since last damage
                        if (currentTime - nexus.getLastDamageTime() >= regenDelay) {
                            if (nexus.getCurrentHealth() < nexus.getMaxHealth()) {
                                nexus.heal(regenRate);
                                plugin.getDataManager().saveKingdomData(kingdom);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 60L); // Run every minute
    }
    
    /**
     * Notify all kingdom members
     */
    private void notifyKingdomMembers(Kingdom kingdom, String messageKey, Object... args) {
        for (UUID memberId : kingdom.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId);
            if (member != null && member.isOnline()) {
                plugin.getMessageManager().sendMessage(member, messageKey, args);
            }
        }
    }
    
    /**
     * Get nexus location for a kingdom
     */
    public Location getNexusLocation(UUID kingdomId) {
        return kingdomNexus.get(kingdomId);
    }
    
    /**
     * Get kingdom ID from nexus location
     */
    public UUID getKingdomFromNexus(Location location) {
        return nexusLocations.get(location);
    }
    
    /**
     * Check if location is a nexus
     */
    public boolean isNexusLocation(Location location) {
        return nexusLocations.containsKey(location);
    }
    
    /**
     * Get all active nexus locations
     */
    public Set<Location> getActiveNexusLocations() {
        return new HashSet<>(activeNexus);
    }
    
    // Event Handlers
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location blockLoc = event.getBlock().getLocation();
        
        // Check if block is part of a nexus structure
        for (Location nexusLoc : activeNexus) {
            if (isPartOfNexusStructure(blockLoc, nexusLoc)) {
                event.setCancelled(true);
                plugin.getMessageManager().sendMessage(event.getPlayer(), "nexus.cannot-break");
                return;
            }
        }
    }
    
    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Location blockLoc = event.getBlock().getLocation();
        
        // Check if block is a nexus beacon
        if (event.getBlock().getType() == Material.BEACON && nexusLocations.containsKey(blockLoc)) {
            event.setCancelled(true);
            
            Player player = event.getPlayer();
            FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
            
            if (feudalPlayer == null || !feudalPlayer.hasKingdom()) {
                plugin.getMessageManager().sendMessage(player, "nexus.no-kingdom");
                return;
            }
            
            Kingdom attackingKingdom = feudalPlayer.getKingdom();
            UUID defendingKingdomId = nexusLocations.get(blockLoc);
            Kingdom defendingKingdom = plugin.getKingdomManager().getKingdom(defendingKingdomId);
            
            if (defendingKingdom == null) {
                return;
            }
            
            // Check if same kingdom
            if (attackingKingdom.getKingdomId().equals(defendingKingdomId)) {
                plugin.getMessageManager().sendMessage(player, "nexus.own-nexus");
                return;
            }
            
            // Check if at war
            if (!areKingdomsAtWar(attackingKingdom, defendingKingdom)) {
                plugin.getMessageManager().sendMessage(player, "nexus.not-at-war", defendingKingdom.getName());
                return;
            }
            
            // Deal damage to nexus
            int damage = calculateNexusDamage(player);
            damageNexus(blockLoc, damage, attackingKingdom);
        }
    }
    
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        // Protect nexus structures from explosions
        event.blockList().removeIf(block -> {
            for (Location nexusLoc : activeNexus) {
                if (isPartOfNexusStructure(block.getLocation(), nexusLoc)) {
                    return true;
                }
            }
            return false;
        });
    }
    
    /**
     * Check if a location is part of a nexus structure
     */
    private boolean isPartOfNexusStructure(Location blockLoc, Location nexusLoc) {
        if (!blockLoc.getWorld().equals(nexusLoc.getWorld())) {
            return false;
        }
        
        int dx = Math.abs(blockLoc.getBlockX() - nexusLoc.getBlockX());
        int dy = Math.abs(blockLoc.getBlockY() - nexusLoc.getBlockY());
        int dz = Math.abs(blockLoc.getBlockZ() - nexusLoc.getBlockZ());
        
        return dx <= 2 && dy <= 1 && dz <= 2;
    }
    
    /**
     * Calculate damage dealt to nexus based on player attributes
     */
    private int calculateNexusDamage(Player player) {
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (feudalPlayer == null) {
            return 10; // Base damage
        }
        
        int baseDamage = 10;
        int strength = feudalPlayer.getAttribute(Attribute.STRENGTH);
        
        // Each strength point above 10 adds 5% damage
        double strengthMultiplier = 1.0 + ((strength - 10) * 0.05);
        
        return (int) (baseDamage * strengthMultiplier);
    }
    
    /**
     * Load nexus data on startup
     */
    public void loadNexusData() {
        nexusLocations.clear();
        kingdomNexus.clear();
        activeNexus.clear();
        
        for (Kingdom kingdom : plugin.getKingdomManager().getAllKingdoms()) {
            if (kingdom.getNexus() != null && kingdom.getNexus().getLocation() != null) {
                Location nexusLoc = kingdom.getNexus().getLocation();
                nexusLocations.put(nexusLoc, kingdom.getKingdomId());
                kingdomNexus.put(kingdom.getKingdomId(), nexusLoc);
                activeNexus.add(nexusLoc);
            }
        }
        
        plugin.getLogger().info("Loaded " + activeNexus.size() + " active nexus locations");
    }
}
