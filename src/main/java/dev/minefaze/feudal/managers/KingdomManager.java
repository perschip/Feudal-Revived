package dev.minefaze.feudal.managers;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class KingdomManager {
    
    private final Feudal plugin;
    private final Map<UUID, Kingdom> kingdoms;
    private final Map<String, UUID> kingdomsByName;
    private final Map<Chunk, Territory> territories;
    
    public KingdomManager(Feudal plugin) {
        this.plugin = plugin;
        this.kingdoms = new HashMap<>();
        this.kingdomsByName = new HashMap<>();
        this.territories = new HashMap<>();
    }
    
    public Kingdom createKingdom(String name, UUID leaderId, Location capital) {
        // Check if kingdom name is already taken
        if (kingdomsByName.containsKey(name.toLowerCase())) {
            return null;
        }
        
        // Check if player already has a kingdom
        FeudalPlayer player = plugin.getPlayerDataManager().getPlayer(leaderId);
        if (player != null && player.hasKingdom()) {
            return null;
        }
        
        // Create new kingdom
        UUID kingdomId = UUID.randomUUID();
        Kingdom kingdom = new Kingdom(kingdomId, name, leaderId, capital);
        
        // Register kingdom
        kingdoms.put(kingdomId, kingdom);
        kingdomsByName.put(name.toLowerCase(), kingdomId);
        
        // Register territories
        for (Territory territory : kingdom.getTerritories()) {
            territories.put(territory.getChunk(), territory);
        }
        
        // Update player data
        if (player != null) {
            player.setKingdom(kingdom);
        }
        
        // Save kingdom data
        plugin.getDataManager().saveKingdomData(kingdom);
        
        return kingdom;
    }
    
    public boolean deleteKingdom(UUID kingdomId) {
        Kingdom kingdom = kingdoms.get(kingdomId);
        if (kingdom == null) return false;
        
        // Remove all territories
        for (Territory territory : kingdom.getTerritories()) {
            territories.remove(territory.getChunk());
        }
        
        // Update all members
        for (UUID memberId : kingdom.getMembers()) {
            FeudalPlayer player = plugin.getPlayerDataManager().getPlayer(memberId);
            if (player != null) {
                player.setKingdom(null);
            }
        }
        
        // Remove from maps
        kingdoms.remove(kingdomId);
        kingdomsByName.remove(kingdom.getName().toLowerCase());
        
        // Delete from storage
        plugin.getDataManager().deleteKingdomData(kingdomId);
        
        return true;
    }
    
    public Kingdom getKingdom(UUID kingdomId) {
        return kingdoms.get(kingdomId);
    }
    
    public Kingdom getKingdomByName(String name) {
        UUID kingdomId = kingdomsByName.get(name.toLowerCase());
        return kingdomId != null ? kingdoms.get(kingdomId) : null;
    }
    
    public Kingdom getPlayerKingdom(UUID playerId) {
        FeudalPlayer player = plugin.getPlayerDataManager().getPlayer(playerId);
        return player != null ? player.getKingdom() : null;
    }
    
    public Territory getTerritoryAt(Chunk chunk) {
        return territories.get(chunk);
    }
    
    public Territory getTerritoryAt(Location location) {
        return getTerritoryAt(location.getChunk());
    }
    
    public boolean claimTerritory(UUID kingdomId, Chunk chunk, TerritoryType type) {
        Kingdom kingdom = kingdoms.get(kingdomId);
        if (kingdom == null) return false;
        
        // Check if chunk is already claimed
        if (territories.containsKey(chunk)) {
            return false;
        }
        
        // Check if kingdom can expand
        if (!kingdom.canExpand()) {
            return false;
        }
        
        // Create new territory
        Territory territory = new Territory(UUID.randomUUID(), kingdomId, chunk, type);
        
        // Add to kingdom and maps
        kingdom.addTerritory(territory);
        territories.put(chunk, territory);
        
        // Save data
        plugin.getDataManager().saveTerritoryData(territory);
        plugin.getDataManager().saveKingdomData(kingdom);
        
        return true;
    }
    
    public boolean transferTerritory(Territory territory, UUID newKingdomId) {
        Kingdom oldKingdom = kingdoms.get(territory.getKingdomId());
        Kingdom newKingdom = kingdoms.get(newKingdomId);
        
        if (oldKingdom == null || newKingdom == null) return false;
        
        // Remove from old kingdom
        oldKingdom.removeTerritory(territory);
        
        // Create new territory for new kingdom
        Territory newTerritory = new Territory(
            UUID.randomUUID(),
            newKingdomId,
            territory.getChunk(),
            territory.getType()
        );
        
        // Add to new kingdom
        newKingdom.addTerritory(newTerritory);
        territories.put(territory.getChunk(), newTerritory);
        
        // Save data
        plugin.getDataManager().saveTerritoryData(newTerritory);
        plugin.getDataManager().saveKingdomData(oldKingdom);
        plugin.getDataManager().saveKingdomData(newKingdom);
        plugin.getDataManager().deleteTerritoryData(territory.getTerritoryId());
        
        return true;
    }
    
    public boolean joinKingdom(UUID playerId, UUID kingdomId) {
        Kingdom kingdom = kingdoms.get(kingdomId);
        FeudalPlayer player = plugin.getPlayerDataManager().getPlayer(playerId);
        
        if (kingdom == null || player == null) return false;
        if (player.hasKingdom()) return false;
        
        // Add player to kingdom
        kingdom.addMember(playerId);
        player.setKingdom(kingdom);
        
        // Save data
        plugin.getDataManager().saveKingdomData(kingdom);
        plugin.getPlayerDataManager().savePlayer(player);
        
        return true;
    }
    
    public boolean leaveKingdom(UUID playerId) {
        FeudalPlayer player = plugin.getPlayerDataManager().getPlayer(playerId);
        if (player == null || !player.hasKingdom()) return false;
        
        Kingdom kingdom = player.getKingdom();
        
        // Check if player is the leader
        if (kingdom.isLeader(playerId)) {
            // Transfer leadership or disband kingdom
            if (kingdom.getMemberCount() > 1) {
                // Find new leader (first member that's not the current leader)
                UUID newLeader = kingdom.getMembers().stream()
                    .filter(id -> !id.equals(playerId))
                    .findFirst()
                    .orElse(null);
                
                if (newLeader != null) {
                    kingdom.setLeader(newLeader);
                }
            } else {
                // Disband kingdom if no other members
                deleteKingdom(kingdom.getKingdomId());
                return true;
            }
        }
        
        // Remove player from kingdom
        kingdom.removeMember(playerId);
        player.setKingdom(null);
        
        // Save data
        plugin.getDataManager().saveKingdomData(kingdom);
        plugin.getPlayerDataManager().savePlayer(player);
        
        return true;
    }
    
    public List<Kingdom> getAllKingdoms() {
        return new ArrayList<>(kingdoms.values());
    }
    
    public List<Territory> getTerritoriesForKingdom(UUID kingdomId) {
        return territories.values().stream()
            .filter(territory -> territory.getKingdomId().equals(kingdomId))
            .collect(Collectors.toList());
    }
    
    public List<Territory> getAllTerritories() {
        return new ArrayList<>(territories.values());
    }
    
    public List<Kingdom> getKingdomsNear(Location location, double radius) {
        return kingdoms.values().stream()
            .filter(k -> k.getCapital() != null)
            .filter(k -> k.getCapital().distance(location) <= radius)
            .collect(Collectors.toList());
    }
    
    public boolean canPlayerBuildAt(UUID playerId, Location location) {
        Territory territory = getTerritoryAt(location);
        if (territory == null) return true; // Wilderness
        
        Kingdom kingdom = kingdoms.get(territory.getKingdomId());
        if (kingdom == null) return false;
        
        return kingdom.isMember(playerId);
    }
    
    public void loadKingdomData() {
        // Load kingdoms from storage
        Map<UUID, Kingdom> loadedKingdoms = plugin.getDataManager().loadAllKingdoms();
        for (Kingdom kingdom : loadedKingdoms.values()) {
            kingdoms.put(kingdom.getKingdomId(), kingdom);
            kingdomsByName.put(kingdom.getName().toLowerCase(), kingdom.getKingdomId());
            
            // Load town hall data
            TownHall townHall = plugin.getDataManager().loadTownHallData(kingdom.getKingdomId());
            if (townHall != null) {
                kingdom.setTownHall(townHall);
            }
            
            // Load nexus data
            Nexus nexus = plugin.getDataManager().loadNexusData(kingdom.getKingdomId());
            if (nexus != null) {
                kingdom.setNexus(nexus);
            }
        }
        
        // Load territories from storage
        Map<UUID, Territory> loadedTerritories = plugin.getDataManager().loadAllTerritories();
        for (Territory territory : loadedTerritories.values()) {
            territories.put(territory.getChunk(), territory);
        }
        
        plugin.getLogger().info("Loaded " + loadedKingdoms.size() + " kingdoms and " + loadedTerritories.size() + " territories");
    }
    
    /**
     * Create kingdom with town hall and claim 3x3 chunks
     */
    public Kingdom createKingdomWithTownHall(String name, UUID leaderId, Location location, TownHall.TownHallType type) {
        // Check if kingdom name already exists
        if (getKingdomByName(name) != null) {
            return null;
        }
        
        UUID kingdomId = UUID.randomUUID();
        
        // Create kingdom
        Kingdom kingdom = new Kingdom(kingdomId, name, leaderId, location);
        
        // Initialize town hall and nexus
        kingdom.initializeTownHallAndNexus(type);
        
        // Claim 3x3 chunks around the location
        Chunk centerChunk = location.getChunk();
        int claimedChunks = 0;
        
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Chunk chunk = location.getWorld().getChunkAt(
                    centerChunk.getX() + x, 
                    centerChunk.getZ() + z
                );
                
                // Check if chunk is already claimed
                if (getTerritoryAt(chunk.getBlock(8, 64, 8).getLocation()) != null) {
                    continue; // Skip already claimed chunks
                }
                
                // Determine territory type
                TerritoryType territoryType;
                if (x == 0 && z == 0) {
                    territoryType = TerritoryType.CAPITAL; // Center chunk is capital
                } else {
                    territoryType = TerritoryType.OUTPOST; // Surrounding chunks are outposts
                }
                
                // Create territory
                Territory territory = new Territory(
                    UUID.randomUUID(),
                    kingdomId,
                    chunk,
                    territoryType
                );
                
                // Add to territories map and kingdom
                territories.put(chunk, territory);
                kingdom.addTerritory(territory);
                claimedChunks++;
            }
        }
        
        // Add kingdom to kingdoms map
        kingdoms.put(kingdomId, kingdom);
        kingdomsByName.put(name.toLowerCase(), kingdomId);
        
        // Update player data
        FeudalPlayer player = plugin.getPlayerDataManager().getOrCreatePlayer(
            plugin.getServer().getPlayer(leaderId)
        );
        player.setKingdom(kingdom);
        
        // Save data
        plugin.getDataManager().saveKingdomData(kingdom);
        plugin.getPlayerDataManager().savePlayer(player);
        
        plugin.getLogger().info("Created kingdom '" + name + "' with " + claimedChunks + " territories and " + 
                               type.getDisplayName() + " town hall");
        
        return kingdom;
    }
}
