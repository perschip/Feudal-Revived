package dev.minefaze.feudal.database;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * YAML file-based database provider
 */
public class YamlDatabaseProvider implements DatabaseProvider {
    
    private final Feudal plugin;
    private File dataFolder;
    private File playersFolder;
    private File kingdomsFolder;
    private File territoriesFolder;
    private File challengesFolder;
    private File alliancesFolder;
    private File townHallsFolder;
    private File nexusFolder;
    
    public YamlDatabaseProvider(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void initialize() {
        // Create data directories
        dataFolder = new File(plugin.getDataFolder(), "data");
        playersFolder = new File(dataFolder, "players");
        kingdomsFolder = new File(dataFolder, "kingdoms");
        territoriesFolder = new File(dataFolder, "territories");
        challengesFolder = new File(dataFolder, "challenges");
        alliancesFolder = new File(dataFolder, "alliances");
        townHallsFolder = new File(dataFolder, "townhalls");
        nexusFolder = new File(dataFolder, "nexus");
        
        createDirectories();
        
        plugin.getLogger().info("YAML database initialized.");
    }
    
    private void createDirectories() {
        if (!dataFolder.exists()) dataFolder.mkdirs();
        if (!playersFolder.exists()) playersFolder.mkdirs();
        if (!kingdomsFolder.exists()) kingdomsFolder.mkdirs();
        if (!territoriesFolder.exists()) territoriesFolder.mkdirs();
        if (!challengesFolder.exists()) challengesFolder.mkdirs();
        if (!alliancesFolder.exists()) alliancesFolder.mkdirs();
        if (!townHallsFolder.exists()) townHallsFolder.mkdirs();
        if (!nexusFolder.exists()) nexusFolder.mkdirs();
    }
    
    @Override
    public void close() {
        // Nothing to close for YAML files
    }
    
    @Override
    public boolean testConnection() {
        return dataFolder.exists() && dataFolder.canWrite();
    }
    
    // Player Data Methods
    @Override
    public void savePlayerData(FeudalPlayer player) {
        try {
            File playerFile = new File(playersFolder, player.getPlayerId().toString() + ".yml");
            FileConfiguration config = new YamlConfiguration();
            
            config.set("playerId", player.getPlayerId().toString());
            config.set("playerName", player.getPlayerName());
            config.set("totalExperience", player.getTotalExperience());
            config.set("inCombat", player.isInCombat());
            
            // Save kingdom reference
            if (player.getKingdom() != null) {
                config.set("kingdomId", player.getKingdom().getKingdomId().toString());
            }
            
            // Save profession levels
            for (Profession profession : Profession.values()) {
                config.set("professions." + profession.name(), player.getProfessionLevel(profession));
            }
            
            // Save attributes
            for (Attribute attribute : Attribute.values()) {
                config.set("attributes." + attribute.name(), player.getAttribute(attribute));
            }
            
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + player.getPlayerName(), e);
        }
    }
    
    @Override
    public FeudalPlayer loadPlayerData(UUID playerId) {
        try {
            File playerFile = new File(playersFolder, playerId.toString() + ".yml");
            if (!playerFile.exists()) return null;
            
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            
            String playerName = config.getString("playerName", "Unknown");
            FeudalPlayer player = new FeudalPlayer(playerId, playerName);
            
            player.setTotalExperience(config.getInt("totalExperience", 0));
            player.setInCombat(config.getBoolean("inCombat", false));
            
            // Load profession levels
            for (Profession profession : Profession.values()) {
                int level = config.getInt("professions." + profession.name(), 1);
                player.setProfessionLevel(profession, level);
            }
            
            // Load attributes
            for (Attribute attribute : Attribute.values()) {
                int value = config.getInt("attributes." + attribute.name(), 10);
                player.setAttribute(attribute, value);
            }
            
            // Restore kingdom reference if exists
            if (config.contains("kingdomId")) {
                String kingdomIdStr = config.getString("kingdomId");
                if (kingdomIdStr != null && !kingdomIdStr.isEmpty()) {
                    try {
                        UUID kingdomId = UUID.fromString(kingdomIdStr);
                        Kingdom kingdom = plugin.getKingdomManager().getKingdom(kingdomId);
                        if (kingdom != null) {
                            player.setKingdom(kingdom);
                            // Ensure the player is in the kingdom's member list
                            if (!kingdom.getMembers().contains(playerId)) {
                                kingdom.addMember(playerId);
                                plugin.getLogger().info("Re-added player " + playerId + " to kingdom " + kingdom.getName());
                            }
                        } else {
                            plugin.getLogger().warning("Kingdom " + kingdomId + " not found for player " + playerId);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid kingdom ID for player " + playerId + ": " + kingdomIdStr);
                    }
                }
            }
            
            return player;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + playerId, e);
            return null;
        }
    }
    
    // Kingdom Data Methods
    @Override
    public void saveKingdomData(Kingdom kingdom) {
        try {
            File kingdomFile = new File(kingdomsFolder, kingdom.getKingdomId().toString() + ".yml");
            FileConfiguration config = new YamlConfiguration();
            
            config.set("kingdomId", kingdom.getKingdomId().toString());
            config.set("name", kingdom.getName());
            config.set("leader", kingdom.getLeader().toString());
            config.set("treasury", kingdom.getTreasury());
            config.set("creationTime", kingdom.getCreationTime());
            
            // Save capital location
            if (kingdom.getCapital() != null) {
                config.set("capital.world", kingdom.getCapital().getWorld().getName());
                config.set("capital.x", kingdom.getCapital().getX());
                config.set("capital.y", kingdom.getCapital().getY());
                config.set("capital.z", kingdom.getCapital().getZ());
            }
            
            // Save members
            config.set("members", kingdom.getMembers().stream().map(UUID::toString).toList());
            
            // Save settings
            for (Map.Entry<String, Object> entry : kingdom.getSettings().entrySet()) {
                config.set("settings." + entry.getKey(), entry.getValue());
            }
            
            config.save(kingdomFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save kingdom data for " + kingdom.getName(), e);
        }
    }
    
    @Override
    public Kingdom loadKingdomData(UUID kingdomId) {
        try {
            File kingdomFile = new File(kingdomsFolder, kingdomId.toString() + ".yml");
            if (!kingdomFile.exists()) return null;
            
            FileConfiguration config = YamlConfiguration.loadConfiguration(kingdomFile);
            
            String name = config.getString("name");
            UUID leader = UUID.fromString(config.getString("leader"));
            
            // Load capital location
            org.bukkit.Location capital = null;
            if (config.contains("capital")) {
                String worldName = config.getString("capital.world");
                org.bukkit.World world = plugin.getServer().getWorld(worldName);
                if (world != null) {
                    double x = config.getDouble("capital.x");
                    double y = config.getDouble("capital.y");
                    double z = config.getDouble("capital.z");
                    capital = new org.bukkit.Location(world, x, y, z);
                }
            }
            
            Kingdom kingdom = new Kingdom(kingdomId, name, leader, capital);
            kingdom.setTreasury(config.getInt("treasury", 0));
            
            // Load members
            for (String memberStr : config.getStringList("members")) {
                UUID memberId = UUID.fromString(memberStr);
                if (!memberId.equals(leader)) { // Leader is already added in constructor
                    kingdom.addMember(memberId);
                }
            }
            
            // Load settings
            if (config.contains("settings")) {
                for (String key : config.getConfigurationSection("settings").getKeys(false)) {
                    kingdom.getSettings().put(key, config.get("settings." + key));
                }
            }
            
            return kingdom;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load kingdom data for " + kingdomId, e);
            return null;
        }
    }
    
    @Override
    public Map<UUID, Kingdom> loadAllKingdoms() {
        Map<UUID, Kingdom> kingdoms = new HashMap<>();
        
        if (!kingdomsFolder.exists()) return kingdoms;
        
        File[] files = kingdomsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    String fileName = file.getName().replace(".yml", "");
                    UUID kingdomId = UUID.fromString(fileName);
                    Kingdom kingdom = loadKingdomData(kingdomId);
                    if (kingdom != null) {
                        kingdoms.put(kingdomId, kingdom);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load kingdom file: " + file.getName(), e);
                }
            }
        }
        
        return kingdoms;
    }
    
    @Override
    public void deleteKingdomData(UUID kingdomId) {
        File kingdomFile = new File(kingdomsFolder, kingdomId.toString() + ".yml");
        if (kingdomFile.exists()) {
            kingdomFile.delete();
        }
    }
    
    // Territory Data Methods
    @Override
    public void saveTerritoryData(Territory territory) {
        try {
            File territoryFile = new File(territoriesFolder, territory.getTerritoryId().toString() + ".yml");
            FileConfiguration config = new YamlConfiguration();
            
            config.set("territoryId", territory.getTerritoryId().toString());
            config.set("kingdomId", territory.getKingdomId().toString());
            config.set("type", territory.getType().name());
            config.set("defenseLevel", territory.getDefenseLevel());
            config.set("claimTime", territory.getClaimTime());
            config.set("underAttack", territory.isUnderAttack());
            
            // Save chunk coordinates
            config.set("chunk.world", territory.getChunk().getWorld().getName());
            config.set("chunk.x", territory.getChunk().getX());
            config.set("chunk.z", territory.getChunk().getZ());
            
            config.save(territoryFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save territory data", e);
        }
    }
    
    @Override
    public Territory loadTerritoryData(UUID territoryId) {
        try {
            File territoryFile = new File(territoriesFolder, territoryId.toString() + ".yml");
            if (!territoryFile.exists()) return null;
            
            FileConfiguration config = YamlConfiguration.loadConfiguration(territoryFile);
            
            UUID kingdomId = UUID.fromString(config.getString("kingdomId"));
            TerritoryType type = TerritoryType.valueOf(config.getString("type"));
            
            // Load chunk
            String worldName = config.getString("chunk.world");
            org.bukkit.World world = plugin.getServer().getWorld(worldName);
            if (world == null) return null;
            
            int chunkX = config.getInt("chunk.x");
            int chunkZ = config.getInt("chunk.z");
            org.bukkit.Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            
            Territory territory = new Territory(territoryId, kingdomId, chunk, type);
            territory.setDefenseLevel(config.getInt("defenseLevel", 1));
            territory.setUnderAttack(config.getBoolean("underAttack", false));
            
            return territory;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load territory data for " + territoryId, e);
            return null;
        }
    }
    
    @Override
    public Map<UUID, Territory> loadAllTerritories() {
        Map<UUID, Territory> territories = new HashMap<>();
        
        if (!territoriesFolder.exists()) return territories;
        
        File[] files = territoriesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    String fileName = file.getName().replace(".yml", "");
                    UUID territoryId = UUID.fromString(fileName);
                    Territory territory = loadTerritoryData(territoryId);
                    if (territory != null) {
                        territories.put(territoryId, territory);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load territory file: " + file.getName(), e);
                }
            }
        }
        
        return territories;
    }
    
    @Override
    public void deleteTerritoryData(UUID territoryId) {
        File territoryFile = new File(territoriesFolder, territoryId.toString() + ".yml");
        if (territoryFile.exists()) {
            territoryFile.delete();
        }
    }
    
    // Challenge Data Methods
    @Override
    public void saveChallengeData(Challenge challenge) {
        try {
            File challengeFile = new File(challengesFolder, challenge.getChallengeId().toString() + ".yml");
            FileConfiguration config = new YamlConfiguration();
            
            config.set("challengeId", challenge.getChallengeId().toString());
            config.set("challenger", challenge.getChallenger().toString());
            config.set("target", challenge.getTarget().toString());
            config.set("type", challenge.getType().name());
            config.set("status", challenge.getStatus().name());
            config.set("creationTime", challenge.getCreationTime());
            config.set("expirationTime", challenge.getExpirationTime());
            config.set("wager", challenge.getWager());
            
            if (challenge.getReason() != null) {
                config.set("reason", challenge.getReason());
            }
            
            if (challenge.getTargetTerritory() != null) {
                config.set("targetTerritory", challenge.getTargetTerritory().getTerritoryId().toString());
            }
            
            if (challenge.getBattleLocation() != null) {
                org.bukkit.Location loc = challenge.getBattleLocation();
                config.set("battleLocation.world", loc.getWorld().getName());
                config.set("battleLocation.x", loc.getX());
                config.set("battleLocation.y", loc.getY());
                config.set("battleLocation.z", loc.getZ());
            }
            
            config.save(challengeFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save challenge data", e);
        }
    }
    
    @Override
    public Challenge loadChallengeData(UUID challengeId) {
        // Implementation would be similar to other load methods
        // Returning null for now as Challenge loading is complex
        return null;
    }
    
    @Override
    public Map<UUID, Challenge> loadAllChallenges() {
        // Implementation would load all challenge files
        return new HashMap<>();
    }
    
    @Override
    public void deleteChallengeData(UUID challengeId) {
        File challengeFile = new File(challengesFolder, challengeId.toString() + ".yml");
        if (challengeFile.exists()) {
            challengeFile.delete();
        }
    }
    
    // Alliance Data Methods
    @Override
    public void saveAllianceData(Alliance alliance) {
        try {
            String fileName = alliance.getKingdom1Id().toString() + "_" + alliance.getKingdom2Id().toString() + ".yml";
            File allianceFile = new File(alliancesFolder, fileName);
            FileConfiguration config = new YamlConfiguration();
            
            config.set("kingdom1", alliance.getKingdom1Id().toString());
            config.set("kingdom2", alliance.getKingdom2Id().toString());
            config.set("type", alliance.getType().name());
            config.set("createdAt", alliance.getCreatedAt().toString());
            config.set("active", alliance.isActive());
            
            config.save(allianceFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save alliance data", e);
        }
    }
    
    @Override
    public Alliance loadAllianceData(UUID kingdomId1, UUID kingdomId2) {
        // Try both possible filename combinations
        String fileName1 = kingdomId1.toString() + "_" + kingdomId2.toString() + ".yml";
        String fileName2 = kingdomId2.toString() + "_" + kingdomId1.toString() + ".yml";
        
        File allianceFile = new File(alliancesFolder, fileName1);
        if (!allianceFile.exists()) {
            allianceFile = new File(alliancesFolder, fileName2);
            if (!allianceFile.exists()) {
                return null;
            }
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(allianceFile);
            
            UUID kingdom1Id = UUID.fromString(config.getString("kingdom1"));
            UUID kingdom2Id = UUID.fromString(config.getString("kingdom2"));
            Alliance.AllianceType type = Alliance.AllianceType.valueOf(config.getString("type"));
            
            Alliance alliance = new Alliance(kingdom1Id, kingdom2Id, type);
            alliance.setActive(config.getBoolean("active", true));
            
            return alliance;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load alliance data from " + allianceFile.getName(), e);
            return null;
        }
    }
    
    @Override
    public Map<String, Alliance> loadAllAlliances() {
        Map<String, Alliance> alliances = new HashMap<>();
        
        if (!alliancesFolder.exists()) {
            return alliances;
        }
        
        File[] allianceFiles = alliancesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (allianceFiles == null) {
            return alliances;
        }
        
        for (File allianceFile : allianceFiles) {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(allianceFile);
                
                UUID kingdom1Id = UUID.fromString(config.getString("kingdom1"));
                UUID kingdom2Id = UUID.fromString(config.getString("kingdom2"));
                Alliance.AllianceType type = Alliance.AllianceType.valueOf(config.getString("type"));
                
                Alliance alliance = new Alliance(kingdom1Id, kingdom2Id, type);
                alliance.setActive(config.getBoolean("active", true));
                
                String key = kingdom1Id.toString() + "_" + kingdom2Id.toString();
                alliances.put(key, alliance);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load alliance data from " + allianceFile.getName(), e);
            }
        }
        
        return alliances;
    }
    
    @Override
    public void deleteAllianceData(UUID kingdomId1, UUID kingdomId2) {
        String fileName = kingdomId1.toString() + "_" + kingdomId2.toString() + ".yml";
        File allianceFile = new File(alliancesFolder, fileName);
        if (allianceFile.exists()) {
            allianceFile.delete();
        }
        
        // Also try reverse order
        fileName = kingdomId2.toString() + "_" + kingdomId1.toString() + ".yml";
        allianceFile = new File(alliancesFolder, fileName);
        if (allianceFile.exists()) {
            allianceFile.delete();
        }
    }
    
    // Town Hall Data Methods
    @Override
    public void saveTownHallData(TownHall townHall, UUID kingdomId) {
        try {
            File townHallFile = new File(townHallsFolder, kingdomId.toString() + ".yml");
            FileConfiguration config = new YamlConfiguration();
            
            config.set("kingdomId", kingdomId.toString());
            config.set("level", townHall.getLevel());
            config.set("type", townHall.getType().name());
            config.set("upgrading", townHall.isUpgrading());
            config.set("upgradeStartTime", townHall.getUpgradeStartTime());
            
            // Save location
            if (townHall.getLocation() != null) {
                org.bukkit.Location loc = townHall.getLocation();
                config.set("location.world", loc.getWorld().getName());
                config.set("location.x", loc.getX());
                config.set("location.y", loc.getY());
                config.set("location.z", loc.getZ());
            }
            
            config.save(townHallFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save town hall data", e);
        }
    }
    
    @Override
    public TownHall loadTownHallData(UUID kingdomId) {
        try {
            File townHallFile = new File(townHallsFolder, kingdomId.toString() + ".yml");
            if (!townHallFile.exists()) return null;
            
            FileConfiguration config = YamlConfiguration.loadConfiguration(townHallFile);
            
            int level = config.getInt("level", 1);
            TownHall.TownHallType type = TownHall.TownHallType.valueOf(config.getString("type", "MEDIEVAL"));
            
            // Load location
            org.bukkit.Location location = null;
            if (config.contains("location")) {
                String worldName = config.getString("location.world");
                org.bukkit.World world = plugin.getServer().getWorld(worldName);
                if (world != null) {
                    double x = config.getDouble("location.x");
                    double y = config.getDouble("location.y");
                    double z = config.getDouble("location.z");
                    location = new org.bukkit.Location(world, x, y, z);
                }
            }
            
            TownHall townHall = new TownHall(kingdomId, location, type);
            townHall.setLevel(level);
            townHall.setUpgrading(config.getBoolean("upgrading", false));
            townHall.setUpgradeStartTime(config.getLong("upgradeStartTime", 0));
            
            return townHall;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load town hall data for " + kingdomId, e);
            return null;
        }
    }
    
    // Nexus Data Methods
    @Override
    public void saveNexusData(Nexus nexus, UUID kingdomId) {
        try {
            File nexusFile = new File(nexusFolder, kingdomId.toString() + ".yml");
            FileConfiguration config = new YamlConfiguration();
            
            config.set("kingdomId", kingdomId.toString());
            config.set("currentHealth", nexus.getCurrentHealth());
            config.set("maxHealth", nexus.getMaxHealth());
            config.set("shieldPoints", nexus.getShieldPoints());
            config.set("armor", nexus.getDefenseStats().getOrDefault("armor", 0));
            config.set("magicResistance", nexus.getDefenseStats().getOrDefault("magic_resistance", 0));
            config.set("regenerationRate", nexus.getDefenseStats().getOrDefault("regeneration_rate", 0));
            config.set("lastDamageTime", nexus.getLastDamageTime());
            config.set("regenerating", nexus.isRegenerating());
            
            config.save(nexusFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save nexus data", e);
        }
    }
    
    @Override
    public Nexus loadNexusData(UUID kingdomId) {
        try {
            File nexusFile = new File(nexusFolder, kingdomId.toString() + ".yml");
            if (!nexusFile.exists()) return null;
            
            FileConfiguration config = YamlConfiguration.loadConfiguration(nexusFile);
            
            // Create nexus with default town hall level (will be updated when kingdom loads)
            Nexus nexus = new Nexus(kingdomId, 1);
            
            // Set loaded values
            nexus.setMaxHealth(config.getInt("maxHealth", 1000));
            nexus.setCurrentHealth(config.getInt("currentHealth", nexus.getMaxHealth()));
            nexus.setShieldPoints(config.getInt("shieldPoints", 0));
            nexus.setLastDamageTime(config.getLong("lastDamageTime", 0));
            nexus.setRegenerating(config.getBoolean("regenerating", false));
            
            // Update defense stats with loaded values
            nexus.getDefenseStats().put("armor", config.getInt("armor", 0));
            nexus.getDefenseStats().put("magic_resistance", config.getInt("magicResistance", 0));
            nexus.getDefenseStats().put("regeneration_rate", config.getInt("regenerationRate", 1));
            
            return nexus;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load nexus data for " + kingdomId, e);
            return null;
        }
    }
    
    @Override
    public void saveAll() {
        plugin.getLogger().info("Saving all YAML data...");
        
        // Save all player data
        if (plugin.getPlayerDataManager() != null) {
            for (FeudalPlayer player : plugin.getPlayerDataManager().getAllPlayers().values()) {
                savePlayerData(player);
            }
        }
        
        // Save all kingdom data
        if (plugin.getKingdomManager() != null) {
            for (Kingdom kingdom : plugin.getKingdomManager().getAllKingdoms()) {
                saveKingdomData(kingdom);
                
                // Save town hall and nexus data
                if (kingdom.getTownHall() != null) {
                    saveTownHallData(kingdom.getTownHall(), kingdom.getKingdomId());
                }
                if (kingdom.getNexus() != null) {
                    saveNexusData(kingdom.getNexus(), kingdom.getKingdomId());
                }
            }
            
            // Save all territories
            for (Territory territory : plugin.getKingdomManager().getAllTerritories()) {
                saveTerritoryData(territory);
            }
        }
        
        // Save all active challenges
        if (plugin.getChallengeManager() != null) {
            for (Challenge challenge : plugin.getChallengeManager().getAllActiveChallenges()) {
                saveChallengeData(challenge);
            }
        }
        
        plugin.getLogger().info("All YAML data saved successfully.");
    }
}
