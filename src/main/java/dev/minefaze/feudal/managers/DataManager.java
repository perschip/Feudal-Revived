package dev.minefaze.feudal.managers;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.database.DatabaseFactory;
import dev.minefaze.feudal.database.DatabaseProvider;
import dev.minefaze.feudal.models.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class DataManager {
    
    private final Feudal plugin;
    private DatabaseProvider databaseProvider;
    
    public DataManager(Feudal plugin) {
        this.plugin = plugin;
    }
    
    public void initialize() {
        // Create database provider based on configuration
        databaseProvider = DatabaseFactory.createProvider(plugin);
        databaseProvider.initialize();
        
        // Test database connection
        if (!databaseProvider.testConnection()) {
            plugin.getLogger().severe(plugin.getMessageManager().getMessage("database.connection-failed"));
        }
        
        String providerName = databaseProvider.getClass().getSimpleName().replace("DatabaseProvider", "");
        plugin.getLogger().info(plugin.getMessageManager().getMessage("database.initialized", providerName));
    }
    
    public void close() {
        if (databaseProvider != null) {
            databaseProvider.close();
        }
    }
    
    // Player Data Methods - delegate to database provider
    public void savePlayerData(FeudalPlayer player) {
        if (databaseProvider != null) {
            databaseProvider.savePlayerData(player);
        }
    }
    
    public FeudalPlayer loadPlayerData(UUID playerId) {
        if (databaseProvider != null) {
            return databaseProvider.loadPlayerData(playerId);
        }
        return null;
    }
    
    // Kingdom Data Methods - delegate to database provider
    public void saveKingdomData(Kingdom kingdom) {
        if (databaseProvider != null) {
            databaseProvider.saveKingdomData(kingdom);
        }
    }
    
    public Kingdom loadKingdomData(UUID kingdomId) {
        if (databaseProvider != null) {
            return databaseProvider.loadKingdomData(kingdomId);
        }
        return null;
    }
    
    public Map<UUID, Kingdom> loadAllKingdoms() {
        if (databaseProvider != null) {
            return databaseProvider.loadAllKingdoms();
        }
        return new HashMap<>();
    }
    
    public void deleteKingdomData(UUID kingdomId) {
        if (databaseProvider != null) {
            databaseProvider.deleteKingdomData(kingdomId);
        }
    }
    
    // Territory Data Methods - delegate to database provider
    public void saveTerritoryData(Territory territory) {
        if (databaseProvider != null) {
            databaseProvider.saveTerritoryData(territory);
        }
    }
    
    public Territory loadTerritoryData(UUID territoryId) {
        if (databaseProvider != null) {
            return databaseProvider.loadTerritoryData(territoryId);
        }
        return null;
    }
    
    public Map<UUID, Territory> loadAllTerritories() {
        if (databaseProvider != null) {
            return databaseProvider.loadAllTerritories();
        }
        return new HashMap<>();
    }
    
    public void deleteTerritoryData(UUID territoryId) {
        if (databaseProvider != null) {
            databaseProvider.deleteTerritoryData(territoryId);
        }
    }
    
    // Challenge Data Methods - delegate to database provider
    public void saveChallengeData(Challenge challenge) {
        if (databaseProvider != null) {
            databaseProvider.saveChallengeData(challenge);
        }
    }
    
    // Additional database provider methods
    public void saveTownHallData(TownHall townHall, UUID kingdomId) {
        if (databaseProvider != null) {
            databaseProvider.saveTownHallData(townHall, kingdomId);
        }
    }
    
    public TownHall loadTownHallData(UUID kingdomId) {
        if (databaseProvider != null) {
            return databaseProvider.loadTownHallData(kingdomId);
        }
        return null;
    }
    
    public void saveNexusData(Nexus nexus, UUID kingdomId) {
        if (databaseProvider != null) {
            databaseProvider.saveNexusData(nexus, kingdomId);
        }
    }
    
    public Nexus loadNexusData(UUID kingdomId) {
        if (databaseProvider != null) {
            return databaseProvider.loadNexusData(kingdomId);
        }
        return null;
    }
    
    public void saveAll() {
        if (databaseProvider != null) {
            databaseProvider.saveAll();
        }
    }
}
