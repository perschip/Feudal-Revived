package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.FeudalPlayer;
import dev.minefaze.feudal.models.Territory;
import dev.minefaze.feudal.models.TerritoryType;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.*;

public class ClaimCommand implements SubCommand {
    
    private final Feudal plugin;
    private static final int MAX_CLAIM_AMOUNT = 25;
    
    public ClaimCommand(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        
        if (!feudalPlayer.hasKingdom()) {
            plugin.getMessageManager().sendMessage(player, "claiming.must-be-in-kingdom");
            return true;
        }
        
        if (!feudalPlayer.isKingdomLeader()) {
            plugin.getMessageManager().sendMessage(player, "claiming.must-be-leader");
            return true;
        }
        
        // Parse amount argument
        int amount = 1;
        if (args.length > 0) {
            try {
                amount = Integer.parseInt(args[0]);
                if (amount < 1 || amount > MAX_CLAIM_AMOUNT) {
                    plugin.getMessageManager().sendMessage(player, "claiming.invalid-amount");
                    return true;
                }
            } catch (NumberFormatException e) {
                plugin.getMessageManager().sendMessage(player, "claiming.invalid-amount");
                return true;
            }
        }
        
        UUID kingdomId = feudalPlayer.getKingdom().getKingdomId();
        Chunk startChunk = player.getLocation().getChunk();
        
        if (amount == 1) {
            // Single chunk claiming
            claimSingleChunk(player, kingdomId, startChunk);
        } else {
            // Multi-chunk claiming
            claimMultipleChunks(player, kingdomId, startChunk, amount);
        }
        
        return true;
    }
    
    /**
     * Claim a single chunk
     */
    private void claimSingleChunk(Player player, UUID kingdomId, Chunk chunk) {
        if (plugin.getKingdomManager().claimTerritory(kingdomId, chunk, TerritoryType.OUTPOST)) {
            plugin.getMessageManager().sendMessage(player, "claiming.single-success");
        } else {
            plugin.getMessageManager().sendMessage(player, "claiming.failed");
        }
    }
    
    /**
     * Claim multiple connected chunks
     */
    private void claimMultipleChunks(Player player, UUID kingdomId, Chunk startChunk, int amount) {
        plugin.getMessageManager().sendMessage(player, "claiming.scanning");
        
        // Get all kingdom territories for connection validation
        Set<Chunk> kingdomChunks = getKingdomChunks(kingdomId);
        
        // Find connected chunks to claim
        List<Chunk> chunksToTry = findConnectedChunks(startChunk, kingdomChunks, amount);
        
        if (chunksToTry.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, "claiming.no-connected-chunks");
            return;
        }
        
        // Attempt to claim chunks
        int successfulClaims = 0;
        for (Chunk chunk : chunksToTry) {
            if (plugin.getKingdomManager().claimTerritory(kingdomId, chunk, TerritoryType.OUTPOST)) {
                successfulClaims++;
                kingdomChunks.add(chunk); // Add to set for future connection checks
            }
        }
        
        // Send appropriate success message
        if (successfulClaims == amount) {
            plugin.getMessageManager().sendMessage(player, "claiming.multi-success", successfulClaims);
        } else if (successfulClaims > 0) {
            plugin.getMessageManager().sendMessage(player, "claiming.partial-success", successfulClaims, amount);
        } else {
            plugin.getMessageManager().sendMessage(player, "claiming.failed");
        }
    }
    
    /**
     * Get all chunks owned by a kingdom
     */
    private Set<Chunk> getKingdomChunks(UUID kingdomId) {
        Set<Chunk> chunks = new HashSet<>();
        // Get all territories for this kingdom
        for (Territory territory : plugin.getKingdomManager().getTerritoriesForKingdom(kingdomId)) {
            chunks.add(territory.getChunk());
        }
        return chunks;
    }
    
    /**
     * Find connected chunks that can be claimed using BFS algorithm
     */
    private List<Chunk> findConnectedChunks(Chunk startChunk, Set<Chunk> kingdomChunks, int maxAmount) {
        List<Chunk> result = new ArrayList<>();
        Set<Chunk> visited = new HashSet<>();
        Queue<Chunk> queue = new LinkedList<>();
        
        // If start chunk is not owned by kingdom, check if it's adjacent to kingdom territory
        if (!kingdomChunks.contains(startChunk)) {
            if (!isAdjacentToKingdomTerritory(startChunk, kingdomChunks)) {
                return result; // Cannot claim disconnected chunks
            }
            queue.offer(startChunk);
        } else {
            // Start from kingdom chunks adjacent to unclaimed areas
            for (Chunk kingdomChunk : kingdomChunks) {
                for (Chunk adjacent : getAdjacentChunks(kingdomChunk)) {
                    if (!kingdomChunks.contains(adjacent) && !visited.contains(adjacent) && 
                        plugin.getKingdomManager().getTerritoryAt(adjacent.getBlock(8, 64, 8).getLocation()) == null) {
                        queue.offer(adjacent);
                        visited.add(adjacent);
                    }
                }
            }
        }
        
        // BFS to find connected claimable chunks
        while (!queue.isEmpty() && result.size() < maxAmount) {
            Chunk current = queue.poll();
            
            // Check if chunk can be claimed
            if (plugin.getKingdomManager().getTerritoryAt(current.getBlock(8, 64, 8).getLocation()) == null) {
                result.add(current);
                
                // Add adjacent chunks to queue
                for (Chunk adjacent : getAdjacentChunks(current)) {
                    if (!visited.contains(adjacent) && !kingdomChunks.contains(adjacent)) {
                        visited.add(adjacent);
                        queue.offer(adjacent);
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Check if a chunk is adjacent to kingdom territory
     */
    private boolean isAdjacentToKingdomTerritory(Chunk chunk, Set<Chunk> kingdomChunks) {
        for (Chunk adjacent : getAdjacentChunks(chunk)) {
            if (kingdomChunks.contains(adjacent)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get all adjacent chunks (4-directional)
     */
    private List<Chunk> getAdjacentChunks(Chunk chunk) {
        List<Chunk> adjacent = new ArrayList<>();
        int x = chunk.getX();
        int z = chunk.getZ();
        
        adjacent.add(chunk.getWorld().getChunkAt(x + 1, z)); // East
        adjacent.add(chunk.getWorld().getChunkAt(x - 1, z)); // West
        adjacent.add(chunk.getWorld().getChunkAt(x, z + 1)); // South
        adjacent.add(chunk.getWorld().getChunkAt(x, z - 1)); // North
        
        return adjacent;
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            // Suggest common amounts
            completions.addAll(Arrays.asList("1", "5", "10", "15", "20", "25"));
        }
        return completions;
    }
    
    @Override
    public String getName() {
        return "claim";
    }
    
    @Override
    public String getDescription() {
        return "Claim territory for your kingdom";
    }
    
    @Override
    public String getUsage() {
        return "[amount]";
    }
}
