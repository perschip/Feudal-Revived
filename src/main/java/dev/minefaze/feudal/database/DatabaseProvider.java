package dev.minefaze.feudal.database;

import dev.minefaze.feudal.models.*;

import java.util.Map;
import java.util.UUID;

/**
 * Database abstraction interface for different storage backends
 */
public interface DatabaseProvider {
    
    /**
     * Initialize the database connection and create tables if needed
     */
    void initialize();
    
    /**
     * Close the database connection
     */
    void close();
    
    /**
     * Test if the database connection is working
     */
    boolean testConnection();
    
    // Player Data Methods
    void savePlayerData(FeudalPlayer player);
    FeudalPlayer loadPlayerData(UUID playerId);
    
    // Kingdom Data Methods
    void saveKingdomData(Kingdom kingdom);
    Kingdom loadKingdomData(UUID kingdomId);
    Map<UUID, Kingdom> loadAllKingdoms();
    void deleteKingdomData(UUID kingdomId);
    
    // Territory Data Methods
    void saveTerritoryData(Territory territory);
    Territory loadTerritoryData(UUID territoryId);
    Map<UUID, Territory> loadAllTerritories();
    void deleteTerritoryData(UUID territoryId);
    
    // Challenge Data Methods
    void saveChallengeData(Challenge challenge);
    Challenge loadChallengeData(UUID challengeId);
    Map<UUID, Challenge> loadAllChallenges();
    void deleteChallengeData(UUID challengeId);
    
    // Alliance Data Methods
    void saveAllianceData(Alliance alliance);
    Alliance loadAllianceData(UUID kingdomId1, UUID kingdomId2);
    Map<String, Alliance> loadAllAlliances();
    void deleteAllianceData(UUID kingdomId1, UUID kingdomId2);
    
    // Town Hall Data Methods
    void saveTownHallData(TownHall townHall, UUID kingdomId);
    TownHall loadTownHallData(UUID kingdomId);
    
    // Nexus Data Methods
    void saveNexusData(Nexus nexus, UUID kingdomId);
    Nexus loadNexusData(UUID kingdomId);
    
    /**
     * Save all data (used for periodic saves and shutdown)
     */
    void saveAll();
}
