package dev.minefaze.feudal.managers;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;

import java.util.*;

/**
 * Manages nexus placement, protection, and war-based damage mechanics
 */
public class NexusManager implements Listener {
    
    private final Feudal plugin;
    private final Map<Location, UUID> nexusLocations; // Location -> Kingdom ID
    private final Map<UUID, Location> kingdomNexus; // Kingdom ID -> Nexus Location
    private final Set<Location> activeNexus; // Currently active nexus locations
    private final Map<Location, EnderCrystal> nexusCrystals; // Location -> End Crystal
    private final Map<Location, List<ArmorStand>> nexusHolograms; // Location -> Hologram lines
    
    public NexusManager(Feudal plugin) {
        this.plugin = plugin;
        this.nexusLocations = new HashMap<>();
        this.kingdomNexus = new HashMap<>();
        this.activeNexus = new HashSet<>();
        this.nexusCrystals = new HashMap<>();
        this.nexusHolograms = new HashMap<>();
        
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
        
        // Check if location is clear and find ground level
        if (!isLocationClear(location)) {
            plugin.getLogger().info("Location not clear for nexus placement");
            plugin.getMessageManager().sendMessage(placer, "nexus.location-blocked");
            return false;
        }
        
        // Find the actual ground level for placement
        Location groundLocation = findGroundLevel(location);
        if (groundLocation == null) {
            plugin.getLogger().info("No suitable ground level found");
            plugin.getMessageManager().sendMessage(placer, "nexus.location-blocked");
            return false;
        }
        
        plugin.getLogger().info("All checks passed, building nexus structure at ground level: " + groundLocation);
        // Place the end beacon structure at ground level
        buildNexusStructure(groundLocation, kingdom);
        
        // Register the nexus at ground level
        nexusLocations.put(groundLocation, kingdom.getKingdomId());
        kingdomNexus.put(kingdom.getKingdomId(), groundLocation);
        activeNexus.add(groundLocation);
        
        // Initialize nexus in kingdom
        if (kingdom.getNexus() == null) {
            int townHallLevel = kingdom.getTownHall() != null ? kingdom.getTownHall().getLevel() : 1;
            Nexus nexus = new Nexus(kingdom.getKingdomId(), townHallLevel);
            nexus.setLocation(groundLocation);
            kingdom.setNexus(nexus);
        } else {
            kingdom.getNexus().setLocation(groundLocation);
        }
        
        // Save data
        plugin.getDataManager().saveKingdomData(kingdom);
        
        // Notify kingdom members
        notifyKingdomMembers(kingdom, "nexus.placed", groundLocation.getBlockX(), groundLocation.getBlockZ());
        
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
     * Build the physical nexus structure (end crystal with hologram)
     */
    private void buildNexusStructure(Location location, Kingdom kingdom) {
        plugin.getLogger().info("Building nexus structure at " + location);
        
        // Spawn the end crystal
        EnderCrystal crystal = (EnderCrystal) location.getWorld().spawnEntity(location, EntityType.END_CRYSTAL);
        crystal.setShowingBottom(false); // Hide the bedrock base
        crystal.setInvulnerable(false); // Allow damage during wars
        crystal.setCustomName(ChatColor.GOLD + kingdom.getName() + " Nexus");
        crystal.setCustomNameVisible(false); // We'll use hologram instead
        
        // Store the crystal
        nexusCrystals.put(location, crystal);
        
        // Create hologram above the crystal
        createNexusHologram(location, kingdom);
        
        plugin.getLogger().info("Nexus structure built successfully with end crystal and hologram");
    }
    
    /**
     * Create hologram display above the nexus
     */
    private void createNexusHologram(Location location, Kingdom kingdom) {
        List<ArmorStand> hologramLines = new ArrayList<>();
        
        // Get nexus health info
        Nexus nexus = kingdom.getNexus();
        int currentHealth = nexus != null ? nexus.getCurrentHealth() : 1000;
        int maxHealth = nexus != null ? nexus.getMaxHealth() : 1000;
        
        // Line 1: Kingdom Name + "Nexus"
        String line1 = ChatColor.GOLD + "" + ChatColor.BOLD + kingdom.getName() + " Nexus";
        ArmorStand hologram1 = createHologramLine(location.clone().add(0, 3, 0), line1);
        hologramLines.add(hologram1);
        
        // Line 2: Health display
        String healthColor = getHealthColor(currentHealth, maxHealth);
        String line2 = ChatColor.WHITE + "Nexus Health: " + healthColor + currentHealth + ChatColor.GRAY + "/" + ChatColor.GREEN + maxHealth;
        ArmorStand hologram2 = createHologramLine(location.clone().add(0, 2.7, 0), line2);
        hologramLines.add(hologram2);
        
        // Store hologram lines
        nexusHolograms.put(location, hologramLines);
        
        plugin.getLogger().info("Created hologram for nexus with " + hologramLines.size() + " lines");
    }
    
    /**
     * Create a single hologram line using armor stand
     */
    private ArmorStand createHologramLine(Location location, String text) {
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand.setCustomName(text);
        armorStand.setCustomNameVisible(true);
        armorStand.setMarker(true); // Makes it non-collidable
        armorStand.setInvulnerable(true);
        return armorStand;
    }
    
    /**
     * Get color for health display based on percentage
     */
    private String getHealthColor(int current, int max) {
        double percentage = (double) current / max;
        if (percentage > 0.75) return ChatColor.GREEN.toString();
        if (percentage > 0.5) return ChatColor.YELLOW.toString();
        if (percentage > 0.25) return ChatColor.GOLD.toString();
        return ChatColor.RED.toString();
    }
    
    /**
     * Update nexus hologram with current health
     */
    public void updateNexusHologram(Location location, Kingdom kingdom) {
        List<ArmorStand> holograms = nexusHolograms.get(location);
        if (holograms == null || holograms.size() < 2) return;
        
        Nexus nexus = kingdom.getNexus();
        if (nexus == null) return;
        
        int currentHealth = nexus.getCurrentHealth();
        int maxHealth = nexus.getMaxHealth();
        
        // Update health line (second hologram)
        String healthColor = getHealthColor(currentHealth, maxHealth);
        String healthText = ChatColor.WHITE + "Nexus Health: " + healthColor + currentHealth + ChatColor.GRAY + "/" + ChatColor.GREEN + maxHealth;
        holograms.get(1).setCustomName(healthText);
    }
    
    /**
     * Destroy the physical nexus structure
     */
    private void destroyNexusStructure(Location location) {
        // Remove the end crystal
        EnderCrystal crystal = nexusCrystals.get(location);
        if (crystal != null && !crystal.isDead()) {
            crystal.remove();
        }
        nexusCrystals.remove(location);
        
        // Remove hologram armor stands
        List<ArmorStand> holograms = nexusHolograms.get(location);
        if (holograms != null) {
            for (ArmorStand hologram : holograms) {
                if (!hologram.isDead()) {
                    hologram.remove();
                }
            }
        }
        nexusHolograms.remove(location);
        
        plugin.getLogger().info("Destroyed nexus structure (crystal and hologram) at " + location);
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
     * Check if a material can be replaced (similar to air)
     */
    private boolean isReplaceable(Material material) {
        return material == Material.AIR ||
               material == Material.CAVE_AIR ||
               material == Material.VOID_AIR ||
               material == Material.WATER ||
               material == Material.LAVA ||
               material == Material.TALL_GRASS ||
               material == Material.SHORT_GRASS ||
               material == Material.FERN ||
               material == Material.LARGE_FERN ||
               material == Material.DEAD_BUSH ||
               material == Material.VINE ||
               material == Material.SNOW ||
               material.name().contains("SAPLING") ||
               material.name().contains("FLOWER");
    }
    
    /**
     * Check if location is clear for nexus placement
     */
    private boolean isLocationClear(Location location) {
        plugin.getLogger().info("Checking if location is clear for nexus placement at " + location);
        
        // Find the ground level - look down up to 10 blocks to find solid ground
        Location groundLocation = findGroundLevel(location);
        if (groundLocation == null) {
            plugin.getLogger().info("No solid ground found within 10 blocks below placement location");
            return false;
        }
        
        // Check if we can place the beacon at ground level
        Block centerBlock = groundLocation.getBlock();
        if (centerBlock.getType() != Material.AIR && !isReplaceable(centerBlock.getType())) {
            plugin.getLogger().info("Center block not suitable for beacon: " + centerBlock.getType());
            return false;
        }
        
        // Check the 5x5 area around the nexus location for major obstructions
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Location checkLoc = groundLocation.clone().add(x, 0, z);
                Block block = checkLoc.getBlock();
                
                // Only fail if there are major obstructions (not replaceable blocks)
                if (block.getType() != Material.AIR && !isReplaceable(block.getType()) && 
                    !block.getType().isSolid()) {
                    // Allow solid blocks as they can be part of the terrain
                    continue;
                }
            }
        }
        
        plugin.getLogger().info("Location is clear for nexus placement at ground level: " + groundLocation);
        return true;
    }
    
    /**
     * Find the ground level by looking down from the given location
     */
    private Location findGroundLevel(Location startLocation) {
        Location checkLocation = startLocation.clone();
        
        // Look down up to 10 blocks to find solid ground
        for (int i = 0; i < 10; i++) {
            Block block = checkLocation.getBlock();
            Block belowBlock = checkLocation.clone().add(0, -1, 0).getBlock();
            
            // If current block is air/replaceable and block below is solid, this is ground level
            if ((block.getType() == Material.AIR || isReplaceable(block.getType())) && 
                belowBlock.getType().isSolid()) {
                plugin.getLogger().info("Found ground level at Y=" + checkLocation.getBlockY());
                return checkLocation;
            }
            
            checkLocation.add(0, -1, 0);
        }
        
        return null; // No ground found within 10 blocks
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
        // Protect nexus crystals from explosions
        for (EnderCrystal crystal : nexusCrystals.values()) {
            if (crystal.getLocation().distance(event.getLocation()) < 10) {
                // Cancel explosion damage to nexus crystals
                event.setCancelled(true);
                return;
            }
        }
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal)) return;
        
        EnderCrystal crystal = (EnderCrystal) event.getEntity();
        
        // Check if this is a nexus crystal
        if (!nexusCrystals.containsValue(crystal)) return;
        
        // Find the exact location key for this crystal
        Location nexusLocation = null;
        for (Map.Entry<Location, EnderCrystal> entry : nexusCrystals.entrySet()) {
            if (entry.getValue().equals(crystal)) {
                nexusLocation = entry.getKey();
                break;
            }
        }
        
        if (nexusLocation == null) return;
        
        UUID kingdomId = nexusLocations.get(nexusLocation);
        if (kingdomId == null) return;
        
        Kingdom kingdom = plugin.getKingdomManager().getKingdom(kingdomId);
        if (kingdom == null) return;
        
        // Handle damage based on attacker type
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            handleNexusDamage(nexusLocation, kingdom, attacker, event);
        } else {
            // Cancel damage from non-players (explosions, etc.)
            event.setCancelled(true);
        }
    }
    
    private void handleNexusDamage(Location nexusLocation, Kingdom kingdom, Player attacker, EntityDamageByEntityEvent event) {
        // Get attacker's kingdom
        FeudalPlayer attackerData = plugin.getPlayerDataManager().getPlayer(attacker.getUniqueId());
        if (attackerData == null) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(attacker, "nexus.no-kingdom");
            return;
        }
        
        Kingdom attackerKingdom = attackerData.getKingdom();
        if (attackerKingdom == null) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(attacker, "nexus.no-kingdom");
            return;
        }
        
        // Can't damage own nexus
        if (attackerKingdom.getKingdomId().equals(kingdom.getKingdomId())) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(attacker, "nexus.own-nexus");
            return;
        }
        
        // Check if kingdoms are at war
        if (!plugin.getAllianceManager().areAtWar(attackerKingdom, kingdom)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(attacker, "nexus.not-at-war", kingdom.getName());
            return;
        }
        
        // Cancel the default damage and apply custom damage
        event.setCancelled(true);
        
        // Calculate damage based on player attributes
        int damage = calculateNexusDamage(attacker);
        
        // Apply damage to nexus
        Nexus nexus = kingdom.getNexus();
        if (nexus != null) {
            int newHealth = Math.max(0, nexus.getCurrentHealth() - damage);
            nexus.setCurrentHealth(newHealth);
            
            // Update hologram
            updateNexusHologram(nexusLocation, kingdom);
            
            // Notify players
            plugin.getMessageManager().sendMessage(attacker, "nexus.damage-dealt", damage, kingdom.getName());
            notifyKingdomMembers(kingdom, "nexus.damaged", nexus.getCurrentHealth(), nexus.getMaxHealth());
            
            // Check if nexus is destroyed
            if (newHealth <= 0) {
                handleNexusDestruction(kingdom, attackerKingdom);
            }
            
            // Save data
            plugin.getDataManager().saveKingdomData(kingdom);
        }
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
