package dev.minefaze.feudal.managers;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Particle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TerritoryVisualizationManager {
    
    private final Feudal plugin;
    private final Map<UUID, Set<Chunk>> activeVisualizations;
    private final Map<UUID, BukkitRunnable> visualizationTasks;
    
    public TerritoryVisualizationManager(Feudal plugin) {
        this.plugin = plugin;
        this.activeVisualizations = new ConcurrentHashMap<>();
        this.visualizationTasks = new ConcurrentHashMap<>();
    }
    
    /**
     * Show territory borders with particles for a player
     */
    public void showTerritoryBorders(Player player, int radius) {
        UUID playerId = player.getUniqueId();
        
        // Stop existing visualization
        stopVisualization(player);
        
        // Get chunks around player
        Set<Chunk> chunksToVisualize = getChunksAroundPlayer(player, radius);
        activeVisualizations.put(playerId, chunksToVisualize);
        
        // Start particle task
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    stopVisualization(player);
                    return;
                }
                
                showParticlesForChunks(player, chunksToVisualize);
            }
        };
        
        task.runTaskTimer(plugin, 0L, 20L); // Run every second
        visualizationTasks.put(playerId, task);
        
        plugin.getMessageManager().sendMessage(player, "visualization.enabled");
    }
    
    /**
     * Stop territory visualization for a player
     */
    public void stopVisualization(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel task
        BukkitRunnable task = visualizationTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        // Clear visualization data
        activeVisualizations.remove(playerId);
        
        plugin.getMessageManager().sendMessage(player, "visualization.disabled");
    }
    
    /**
     * Toggle territory visualization for a player
     */
    public void toggleVisualization(Player player, int radius) {
        if (activeVisualizations.containsKey(player.getUniqueId())) {
            stopVisualization(player);
        } else {
            showTerritoryBorders(player, radius);
        }
    }
    
    /**
     * Show territory map in chat for a player
     */
    public void showTerritoryMap(Player player, int radius) {
        Chunk centerChunk = player.getLocation().getChunk();
        World world = player.getWorld();
        
        plugin.getMessageManager().sendMessage(player, "map.header");
        plugin.getMessageManager().sendMessage(player, "map.center", centerChunk.getX(), centerChunk.getZ());
        plugin.getMessageManager().sendMessage(player, "map.legend");
        player.sendMessage("");
        
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        UUID playerKingdomId = feudalPlayer.hasKingdom() ? feudalPlayer.getKingdom().getKingdomId() : null;
        
        // Create map grid
        StringBuilder mapBuilder = new StringBuilder();
        for (int z = -radius; z <= radius; z++) {
            for (int x = -radius; x <= radius; x++) {
                Chunk chunk = world.getChunkAt(centerChunk.getX() + x, centerChunk.getZ() + z);
                Territory territory = plugin.getKingdomManager().getTerritoryAt(chunk.getBlock(8, 64, 8).getLocation());
                
                String symbol = getMapSymbol(territory, playerKingdomId);
                mapBuilder.append(symbol);
            }
            mapBuilder.append("\n");
        }
        
        player.sendMessage(mapBuilder.toString());
        
        // Show territory counts
        showTerritoryStats(player, centerChunk, radius);
    }
    
    /**
     * Get chunks around a player within radius
     */
    private Set<Chunk> getChunksAroundPlayer(Player player, int radius) {
        Set<Chunk> chunks = new HashSet<>();
        Chunk centerChunk = player.getLocation().getChunk();
        World world = player.getWorld();
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                chunks.add(world.getChunkAt(centerChunk.getX() + x, centerChunk.getZ() + z));
            }
        }
        
        return chunks;
    }
    
    /**
     * Show particles for claimed chunks
     */
    private void showParticlesForChunks(Player player, Set<Chunk> chunks) {
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        UUID playerKingdomId = feudalPlayer.hasKingdom() ? feudalPlayer.getKingdom().getKingdomId() : null;
        
        for (Chunk chunk : chunks) {
            Territory territory = plugin.getKingdomManager().getTerritoryAt(
                chunk.getBlock(8, 64, 8).getLocation());
            
            if (territory != null) {
                showChunkBorderParticles(player, chunk, territory, playerKingdomId);
            }
        }
    }
    
    /**
     * Show particle border around a chunk
     */
    private void showChunkBorderParticles(Player player, Chunk chunk, Territory territory, UUID playerKingdomId) {
        World world = chunk.getWorld();
        int minX = chunk.getX() * 16;
        int minZ = chunk.getZ() * 16;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        
        // Determine particle color based on relationship
        Particle.DustOptions dustOptions = getParticleColor(territory, playerKingdomId);
        
        // Get appropriate Y level (highest block + 1)
        int y = world.getHighestBlockYAt(minX + 8, minZ + 8) + 2;
        
        // Show enhanced corner particles (more visible with multiple particles)
        spawnEnhancedParticle(player, world, minX, y, minZ, dustOptions);
        spawnEnhancedParticle(player, world, maxX, y, minZ, dustOptions);
        spawnEnhancedParticle(player, world, minX, y, maxZ, dustOptions);
        spawnEnhancedParticle(player, world, maxX, y, maxZ, dustOptions);
        
        // Show dense border particles every 2 blocks for better visibility
        for (int i = 0; i <= 16; i += 2) {
            // North and South borders
            spawnEnhancedParticle(player, world, minX + i, y, minZ, dustOptions);
            spawnEnhancedParticle(player, world, minX + i, y, maxZ, dustOptions);
            
            // East and West borders  
            spawnEnhancedParticle(player, world, minX, y, minZ + i, dustOptions);
            spawnEnhancedParticle(player, world, maxX, y, minZ + i, dustOptions);
        }
        
        // Add vertical particle pillars at corners for better visibility
        for (int yOffset = 0; yOffset <= 3; yOffset++) {
            spawnParticle(player, world, minX, y + yOffset, minZ, dustOptions);
            spawnParticle(player, world, maxX, y + yOffset, minZ, dustOptions);
            spawnParticle(player, world, minX, y + yOffset, maxZ, dustOptions);
            spawnParticle(player, world, maxX, y + yOffset, maxZ, dustOptions);
        }
        
        // Show territory type indicator at center
        Location center = new Location(world, minX + 8, y + 1, minZ + 8);
        Particle centerParticle = getCenterParticle(territory.getType());
        player.spawnParticle(centerParticle, center, 3, 0.5, 0.5, 0.5, 0.1);
    }
    
    /**
     * Spawn a particle for a specific player
     */
    private void spawnParticle(Player player, World world, int x, int y, int z, Particle.DustOptions dustOptions) {
        Location loc = new Location(world, x, y, z);
        player.spawnParticle(Particle.DUST, loc, 1, dustOptions);
    }
    
    /**
     * Spawn enhanced particles with multiple effects for better visibility
     */
    private void spawnEnhancedParticle(Player player, World world, int x, int y, int z, Particle.DustOptions dustOptions) {
        Location loc = new Location(world, x, y, z);
        // Main dust particle
        player.spawnParticle(Particle.DUST, loc, 2, 0.1, 0.1, 0.1, 0, dustOptions);
        // Add a subtle glow effect
        player.spawnParticle(Particle.END_ROD, loc, 1, 0.1, 0.1, 0.1, 0.01);
    }
    
    /**
     * Get particle color based on territory relationship
     */
    private Particle.DustOptions getParticleColor(Territory territory, UUID playerKingdomId) {
        if (playerKingdomId == null) {
            return new Particle.DustOptions(Color.GRAY, 1.0f); // Neutral for non-kingdom players
        }
        
        UUID territoryOwnerId = territory.getKingdomId();
        
        // Own territory
        if (territoryOwnerId.equals(playerKingdomId)) {
            return new Particle.DustOptions(Color.GREEN, 1.0f);
        }
        
        // Nation ally
        if (plugin.getAllianceManager().areNationAllies(playerKingdomId, territoryOwnerId)) {
            return new Particle.DustOptions(Color.AQUA, 1.0f);
        }
        
        // Direct ally
        if (plugin.getAllianceManager().areAllies(playerKingdomId, territoryOwnerId)) {
            return new Particle.DustOptions(Color.BLUE, 1.0f);
        }
        
        // Enemy
        if (plugin.getAllianceManager().areEnemies(playerKingdomId, territoryOwnerId)) {
            return new Particle.DustOptions(Color.RED, 1.0f);
        }
        
        // Neutral
        return new Particle.DustOptions(Color.YELLOW, 1.0f);
    }
    
    /**
     * Get center particle for territory type
     */
    private Particle getCenterParticle(TerritoryType type) {
        return switch (type) {
            case CAPITAL -> Particle.HAPPY_VILLAGER;
            case OUTPOST -> Particle.CRIT;
            case FORTRESS -> Particle.FLAME;
            case TOWN -> Particle.HAPPY_VILLAGER;
            case FARM -> Particle.COMPOSTER;
            case MINE -> Particle.LAVA;
        };
    }
    
    /**
     * Get map symbol for territory
     */
    private String getMapSymbol(Territory territory, UUID playerKingdomId) {
        if (territory == null) {
            return "§8■"; // Unclaimed
        }
        
        if (playerKingdomId == null) {
            return "§e■"; // Neutral for non-kingdom players
        }
        
        UUID territoryOwnerId = territory.getKingdomId();
        
        // Own territory
        if (territoryOwnerId.equals(playerKingdomId)) {
            return "§a■";
        }
        
        // Nation ally
        if (plugin.getAllianceManager().areNationAllies(playerKingdomId, territoryOwnerId)) {
            return "§b■";
        }
        
        // Direct ally
        if (plugin.getAllianceManager().areAllies(playerKingdomId, territoryOwnerId)) {
            return "§9■";
        }
        
        // Enemy
        if (plugin.getAllianceManager().areEnemies(playerKingdomId, territoryOwnerId)) {
            return "§c■";
        }
        
        // Neutral
        return "§e■";
    }
    
    /**
     * Show territory statistics
     */
    private void showTerritoryStats(Player player, Chunk centerChunk, int radius) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("own", 0);
        stats.put("ally", 0);
        stats.put("enemy", 0);
        stats.put("neutral", 0);
        stats.put("unclaimed", 0);
        
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        UUID playerKingdomId = feudalPlayer.hasKingdom() ? feudalPlayer.getKingdom().getKingdomId() : null;
        
        World world = player.getWorld();
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Chunk chunk = world.getChunkAt(centerChunk.getX() + x, centerChunk.getZ() + z);
                Territory territory = plugin.getKingdomManager().getTerritoryAt(
                    chunk.getBlock(8, 64, 8).getLocation());
                
                if (territory == null) {
                    stats.put("unclaimed", stats.get("unclaimed") + 1);
                } else if (playerKingdomId != null) {
                    UUID territoryOwnerId = territory.getKingdomId();
                    
                    if (territoryOwnerId.equals(playerKingdomId)) {
                        stats.put("own", stats.get("own") + 1);
                    } else if (plugin.getAllianceManager().areNationAllies(playerKingdomId, territoryOwnerId) ||
                               plugin.getAllianceManager().areAllies(playerKingdomId, territoryOwnerId)) {
                        stats.put("ally", stats.get("ally") + 1);
                    } else if (plugin.getAllianceManager().areEnemies(playerKingdomId, territoryOwnerId)) {
                        stats.put("enemy", stats.get("enemy") + 1);
                    } else {
                        stats.put("neutral", stats.get("neutral") + 1);
                    }
                } else {
                    stats.put("neutral", stats.get("neutral") + 1);
                }
            }
        }
        
        player.sendMessage("");
        plugin.getMessageManager().sendMessage(player, "map.statistics-header");
        plugin.getMessageManager().sendMessage(player, "map.stats-own", stats.get("own"));
        plugin.getMessageManager().sendMessage(player, "map.stats-allied", stats.get("ally"));
        plugin.getMessageManager().sendMessage(player, "map.stats-enemy", stats.get("enemy"));
        plugin.getMessageManager().sendMessage(player, "map.stats-neutral", stats.get("neutral"));
        plugin.getMessageManager().sendMessage(player, "map.stats-unclaimed", stats.get("unclaimed"));
    }
    
    /**
     * Clean up when plugin disables
     */
    public void shutdown() {
        // Cancel all running tasks
        for (BukkitRunnable task : visualizationTasks.values()) {
            task.cancel();
        }
        visualizationTasks.clear();
        activeVisualizations.clear();
    }
    
    /**
     * Check if player has visualization active
     */
    public boolean hasVisualizationActive(UUID playerId) {
        return activeVisualizations.containsKey(playerId);
    }
}
