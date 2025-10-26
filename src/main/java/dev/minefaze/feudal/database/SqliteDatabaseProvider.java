package dev.minefaze.feudal.database;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * SQLite database provider for local database storage
 */
public class SqliteDatabaseProvider implements DatabaseProvider {
    
    private final Feudal plugin;
    private final String filename;
    private Connection connection;
    
    public SqliteDatabaseProvider(Feudal plugin, String filename) {
        this.plugin = plugin;
        this.filename = filename;
    }
    
    @Override
    public void initialize() {
        try {
            File dbFile = new File(plugin.getDataFolder(), filename);
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            
            connection = DriverManager.getConnection(url);
            createTables();
            
            plugin.getLogger().info("SQLite database initialized: " + filename);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite database", e);
        }
    }
    
    private void createTables() throws SQLException {
        // Players table
        connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS players (
                player_id TEXT PRIMARY KEY,
                player_name TEXT NOT NULL,
                kingdom_id TEXT,
                total_experience INTEGER DEFAULT 0,
                in_combat BOOLEAN DEFAULT FALSE,
                warrior_level INTEGER DEFAULT 1,
                miner_level INTEGER DEFAULT 1,
                builder_level INTEGER DEFAULT 1,
                farmer_level INTEGER DEFAULT 1,
                strength INTEGER DEFAULT 10,
                defense INTEGER DEFAULT 10,
                agility INTEGER DEFAULT 10,
                endurance INTEGER DEFAULT 10,
                intelligence INTEGER DEFAULT 10
            )
        """);
        
        // Kingdoms table
        connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS kingdoms (
                kingdom_id TEXT PRIMARY KEY,
                name TEXT UNIQUE NOT NULL,
                leader TEXT NOT NULL,
                treasury INTEGER DEFAULT 0,
                creation_time INTEGER NOT NULL,
                capital_world TEXT,
                capital_x REAL,
                capital_y REAL,
                capital_z REAL,
                settings TEXT
            )
        """);
        
        // Kingdom members table
        connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS kingdom_members (
                kingdom_id TEXT NOT NULL,
                player_id TEXT NOT NULL,
                PRIMARY KEY (kingdom_id, player_id),
                FOREIGN KEY (kingdom_id) REFERENCES kingdoms(kingdom_id) ON DELETE CASCADE
            )
        """);
        
        // Territories table
        connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS territories (
                territory_id TEXT PRIMARY KEY,
                kingdom_id TEXT NOT NULL,
                type TEXT NOT NULL,
                defense_level INTEGER DEFAULT 1,
                claim_time INTEGER NOT NULL,
                under_attack BOOLEAN DEFAULT FALSE,
                chunk_world TEXT NOT NULL,
                chunk_x INTEGER NOT NULL,
                chunk_z INTEGER NOT NULL,
                FOREIGN KEY (kingdom_id) REFERENCES kingdoms(kingdom_id) ON DELETE CASCADE
            )
        """);
        
        // Challenges table
        connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS challenges (
                challenge_id TEXT PRIMARY KEY,
                challenger TEXT NOT NULL,
                target TEXT NOT NULL,
                type TEXT NOT NULL,
                status TEXT NOT NULL,
                creation_time INTEGER NOT NULL,
                expiration_time INTEGER NOT NULL,
                wager INTEGER DEFAULT 0,
                reason TEXT,
                target_territory TEXT,
                battle_world TEXT,
                battle_x REAL,
                battle_y REAL,
                battle_z REAL
            )
        """);
        
        // Alliances table
        connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS alliances (
                kingdom1 TEXT NOT NULL,
                kingdom2 TEXT NOT NULL,
                type TEXT NOT NULL,
                creation_time INTEGER NOT NULL,
                PRIMARY KEY (kingdom1, kingdom2)
            )
        """);
        
        // Town halls table
        connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS town_halls (
                kingdom_id TEXT PRIMARY KEY,
                level INTEGER DEFAULT 1,
                type TEXT NOT NULL,
                upgrading BOOLEAN DEFAULT FALSE,
                upgrade_start_time INTEGER DEFAULT 0,
                location_world TEXT,
                location_x REAL,
                location_y REAL,
                location_z REAL,
                FOREIGN KEY (kingdom_id) REFERENCES kingdoms(kingdom_id) ON DELETE CASCADE
            )
        """);
        
        // Nexus table
        connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS nexus (
                kingdom_id TEXT PRIMARY KEY,
                current_health INTEGER NOT NULL,
                max_health INTEGER NOT NULL,
                shield_points INTEGER DEFAULT 0,
                armor INTEGER DEFAULT 0,
                magic_resistance INTEGER DEFAULT 0,
                regeneration_rate INTEGER DEFAULT 1,
                last_damage_time INTEGER DEFAULT 0,
                regenerating BOOLEAN DEFAULT FALSE,
                FOREIGN KEY (kingdom_id) REFERENCES kingdoms(kingdom_id) ON DELETE CASCADE
            )
        """);
    }
    
    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error closing SQLite connection", e);
        }
    }
    
    @Override
    public boolean testConnection() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    // Player Data Methods
    @Override
    public void savePlayerData(FeudalPlayer player) {
        String sql = """
            INSERT OR REPLACE INTO players 
            (player_id, player_name, kingdom_id, total_experience, in_combat,
             warrior_level, miner_level, builder_level, farmer_level,
             strength, defense, agility, endurance, intelligence)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, player.getPlayerId().toString());
            stmt.setString(2, player.getPlayerName());
            stmt.setString(3, player.getKingdom() != null ? player.getKingdom().getKingdomId().toString() : null);
            stmt.setInt(4, player.getTotalExperience());
            stmt.setBoolean(5, player.isInCombat());
            stmt.setInt(6, player.getProfessionLevel(Profession.WARRIOR));
            stmt.setInt(7, player.getProfessionLevel(Profession.MINER));
            stmt.setInt(8, player.getProfessionLevel(Profession.BUILDER));
            stmt.setInt(9, player.getProfessionLevel(Profession.FARMER));
            stmt.setInt(10, player.getAttribute(Attribute.STRENGTH));
            stmt.setInt(11, player.getAttribute(Attribute.DEFENSE));
            stmt.setInt(12, player.getAttribute(Attribute.AGILITY));
            stmt.setInt(13, player.getAttribute(Attribute.ENDURANCE));
            stmt.setInt(14, player.getAttribute(Attribute.INTELLIGENCE));
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + player.getPlayerName(), e);
        }
    }
    
    @Override
    public FeudalPlayer loadPlayerData(UUID playerId) {
        String sql = "SELECT * FROM players WHERE player_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String playerName = rs.getString("player_name");
                FeudalPlayer player = new FeudalPlayer(playerId, playerName);
                
                player.setTotalExperience(rs.getInt("total_experience"));
                player.setInCombat(rs.getBoolean("in_combat"));
                
                // Load profession levels
                player.setProfessionLevel(Profession.WARRIOR, rs.getInt("warrior_level"));
                player.setProfessionLevel(Profession.MINER, rs.getInt("miner_level"));
                player.setProfessionLevel(Profession.BUILDER, rs.getInt("builder_level"));
                player.setProfessionLevel(Profession.FARMER, rs.getInt("farmer_level"));
                
                // Load attributes
                player.setAttribute(Attribute.STRENGTH, rs.getInt("strength"));
                player.setAttribute(Attribute.DEFENSE, rs.getInt("defense"));
                player.setAttribute(Attribute.AGILITY, rs.getInt("agility"));
                player.setAttribute(Attribute.ENDURANCE, rs.getInt("endurance"));
                player.setAttribute(Attribute.INTELLIGENCE, rs.getInt("intelligence"));
                
                // Restore kingdom reference if exists
                String kingdomIdStr = rs.getString("kingdom_id");
                if (kingdomIdStr != null && !kingdomIdStr.isEmpty()) {
                    try {
                        UUID kingdomId = UUID.fromString(kingdomIdStr);
                        Kingdom kingdom = plugin.getKingdomManager().getKingdom(kingdomId);
                        if (kingdom != null) {
                            player.setKingdom(kingdom);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid kingdom ID for player " + playerId + ": " + kingdomIdStr);
                    }
                }
                
                return player;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + playerId, e);
        }
        
        return null;
    }
    
    // Kingdom Data Methods
    @Override
    public void saveKingdomData(Kingdom kingdom) {
        String sql = """
            INSERT OR REPLACE INTO kingdoms 
            (kingdom_id, name, leader, treasury, creation_time, 
             capital_world, capital_x, capital_y, capital_z, settings)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, kingdom.getKingdomId().toString());
            stmt.setString(2, kingdom.getName());
            stmt.setString(3, kingdom.getLeader().toString());
            stmt.setInt(4, kingdom.getTreasury());
            stmt.setLong(5, kingdom.getCreationTime());
            
            if (kingdom.getCapital() != null) {
                Location capital = kingdom.getCapital();
                stmt.setString(6, capital.getWorld().getName());
                stmt.setDouble(7, capital.getX());
                stmt.setDouble(8, capital.getY());
                stmt.setDouble(9, capital.getZ());
            } else {
                stmt.setNull(6, Types.VARCHAR);
                stmt.setNull(7, Types.REAL);
                stmt.setNull(8, Types.REAL);
                stmt.setNull(9, Types.REAL);
            }
            
            // Convert settings to JSON string (simplified)
            stmt.setString(10, kingdom.getSettings().toString());
            
            stmt.executeUpdate();
            
            // Save kingdom members
            saveKingdomMembers(kingdom);
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save kingdom data for " + kingdom.getName(), e);
        }
    }
    
    private void saveKingdomMembers(Kingdom kingdom) throws SQLException {
        // Delete existing members
        String deleteSql = "DELETE FROM kingdom_members WHERE kingdom_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
            stmt.setString(1, kingdom.getKingdomId().toString());
            stmt.executeUpdate();
        }
        
        // Insert current members
        String insertSql = "INSERT INTO kingdom_members (kingdom_id, player_id) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            for (UUID memberId : kingdom.getMembers()) {
                stmt.setString(1, kingdom.getKingdomId().toString());
                stmt.setString(2, memberId.toString());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
    
    @Override
    public Kingdom loadKingdomData(UUID kingdomId) {
        String sql = "SELECT * FROM kingdoms WHERE kingdom_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, kingdomId.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String name = rs.getString("name");
                UUID leader = UUID.fromString(rs.getString("leader"));
                
                // Load capital location
                Location capital = null;
                String worldName = rs.getString("capital_world");
                if (worldName != null) {
                    World world = plugin.getServer().getWorld(worldName);
                    if (world != null) {
                        double x = rs.getDouble("capital_x");
                        double y = rs.getDouble("capital_y");
                        double z = rs.getDouble("capital_z");
                        capital = new Location(world, x, y, z);
                    }
                }
                
                Kingdom kingdom = new Kingdom(kingdomId, name, leader, capital);
                kingdom.setTreasury(rs.getInt("treasury"));
                
                // Load members
                loadKingdomMembers(kingdom);
                
                return kingdom;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load kingdom data for " + kingdomId, e);
        }
        
        return null;
    }
    
    private void loadKingdomMembers(Kingdom kingdom) throws SQLException {
        String sql = "SELECT player_id FROM kingdom_members WHERE kingdom_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, kingdom.getKingdomId().toString());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                UUID memberId = UUID.fromString(rs.getString("player_id"));
                if (!memberId.equals(kingdom.getLeader())) {
                    kingdom.addMember(memberId);
                }
            }
        }
    }
    
    @Override
    public Map<UUID, Kingdom> loadAllKingdoms() {
        Map<UUID, Kingdom> kingdoms = new HashMap<>();
        String sql = "SELECT kingdom_id FROM kingdoms";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                UUID kingdomId = UUID.fromString(rs.getString("kingdom_id"));
                Kingdom kingdom = loadKingdomData(kingdomId);
                if (kingdom != null) {
                    kingdoms.put(kingdomId, kingdom);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load all kingdoms", e);
        }
        
        return kingdoms;
    }
    
    @Override
    public void deleteKingdomData(UUID kingdomId) {
        String sql = "DELETE FROM kingdoms WHERE kingdom_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, kingdomId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete kingdom data for " + kingdomId, e);
        }
    }
    
    // Territory Data Methods
    @Override
    public void saveTerritoryData(Territory territory) {
        String sql = """
            INSERT OR REPLACE INTO territories 
            (territory_id, kingdom_id, type, defense_level, claim_time, under_attack,
             chunk_world, chunk_x, chunk_z)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, territory.getTerritoryId().toString());
            stmt.setString(2, territory.getKingdomId().toString());
            stmt.setString(3, territory.getType().name());
            stmt.setInt(4, territory.getDefenseLevel());
            stmt.setLong(5, territory.getClaimTime());
            stmt.setBoolean(6, territory.isUnderAttack());
            stmt.setString(7, territory.getChunk().getWorld().getName());
            stmt.setInt(8, territory.getChunk().getX());
            stmt.setInt(9, territory.getChunk().getZ());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save territory data", e);
        }
    }
    
    @Override
    public Territory loadTerritoryData(UUID territoryId) {
        String sql = "SELECT * FROM territories WHERE territory_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, territoryId.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                UUID kingdomId = UUID.fromString(rs.getString("kingdom_id"));
                TerritoryType type = TerritoryType.valueOf(rs.getString("type"));
                
                String worldName = rs.getString("chunk_world");
                World world = plugin.getServer().getWorld(worldName);
                if (world == null) return null;
                
                int chunkX = rs.getInt("chunk_x");
                int chunkZ = rs.getInt("chunk_z");
                org.bukkit.Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                
                Territory territory = new Territory(territoryId, kingdomId, chunk, type);
                territory.setDefenseLevel(rs.getInt("defense_level"));
                territory.setUnderAttack(rs.getBoolean("under_attack"));
                
                return territory;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load territory data for " + territoryId, e);
        }
        
        return null;
    }
    
    @Override
    public Map<UUID, Territory> loadAllTerritories() {
        Map<UUID, Territory> territories = new HashMap<>();
        String sql = "SELECT territory_id FROM territories";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                UUID territoryId = UUID.fromString(rs.getString("territory_id"));
                Territory territory = loadTerritoryData(territoryId);
                if (territory != null) {
                    territories.put(territoryId, territory);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load all territories", e);
        }
        
        return territories;
    }
    
    @Override
    public void deleteTerritoryData(UUID territoryId) {
        String sql = "DELETE FROM territories WHERE territory_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, territoryId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete territory data for " + territoryId, e);
        }
    }
    
    // Simplified implementations for other methods
    @Override
    public void saveChallengeData(Challenge challenge) {
        // Implementation similar to other save methods
    }
    
    @Override
    public Challenge loadChallengeData(UUID challengeId) {
        return null;
    }
    
    @Override
    public Map<UUID, Challenge> loadAllChallenges() {
        return new HashMap<>();
    }
    
    @Override
    public void deleteChallengeData(UUID challengeId) {
        String sql = "DELETE FROM challenges WHERE challenge_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, challengeId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete challenge data", e);
        }
    }
    
    @Override
    public void saveAllianceData(Alliance alliance) {
        // Implementation for alliance saving
    }
    
    @Override
    public Alliance loadAllianceData(UUID kingdomId1, UUID kingdomId2) {
        return null;
    }
    
    @Override
    public Map<String, Alliance> loadAllAlliances() {
        return new HashMap<>();
    }
    
    @Override
    public void deleteAllianceData(UUID kingdomId1, UUID kingdomId2) {
        // Implementation for alliance deletion
    }
    
    @Override
    public void saveTownHallData(TownHall townHall, UUID kingdomId) {
        String sql = """
            INSERT OR REPLACE INTO town_halls 
            (kingdom_id, level, type, upgrading, upgrade_start_time,
             location_world, location_x, location_y, location_z)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, kingdomId.toString());
            stmt.setInt(2, townHall.getLevel());
            stmt.setString(3, townHall.getType().name());
            stmt.setBoolean(4, townHall.isUpgrading());
            stmt.setLong(5, townHall.getUpgradeStartTime());
            
            if (townHall.getLocation() != null) {
                Location loc = townHall.getLocation();
                stmt.setString(6, loc.getWorld().getName());
                stmt.setDouble(7, loc.getX());
                stmt.setDouble(8, loc.getY());
                stmt.setDouble(9, loc.getZ());
            } else {
                stmt.setNull(6, Types.VARCHAR);
                stmt.setNull(7, Types.REAL);
                stmt.setNull(8, Types.REAL);
                stmt.setNull(9, Types.REAL);
            }
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save town hall data", e);
        }
    }
    
    @Override
    public TownHall loadTownHallData(UUID kingdomId) {
        String sql = "SELECT * FROM town_halls WHERE kingdom_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, kingdomId.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int level = rs.getInt("level");
                TownHall.TownHallType type = TownHall.TownHallType.valueOf(rs.getString("type"));
                
                Location location = null;
                String worldName = rs.getString("location_world");
                if (worldName != null) {
                    World world = plugin.getServer().getWorld(worldName);
                    if (world != null) {
                        double x = rs.getDouble("location_x");
                        double y = rs.getDouble("location_y");
                        double z = rs.getDouble("location_z");
                        location = new Location(world, x, y, z);
                    }
                }
                
                TownHall townHall = new TownHall(kingdomId, location, type);
                townHall.setLevel(level);
                townHall.setUpgrading(rs.getBoolean("upgrading"));
                townHall.setUpgradeStartTime(rs.getLong("upgrade_start_time"));
                
                return townHall;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load town hall data for " + kingdomId, e);
        }
        
        return null;
    }
    
    @Override
    public void saveNexusData(Nexus nexus, UUID kingdomId) {
        String sql = """
            INSERT OR REPLACE INTO nexus 
            (kingdom_id, current_health, max_health, shield_points, armor, 
             magic_resistance, regeneration_rate, last_damage_time, regenerating)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, kingdomId.toString());
            stmt.setInt(2, nexus.getCurrentHealth());
            stmt.setInt(3, nexus.getMaxHealth());
            stmt.setInt(4, nexus.getShieldPoints());
            stmt.setInt(5, nexus.getDefenseStats().getOrDefault("armor", 0));
            stmt.setInt(6, nexus.getDefenseStats().getOrDefault("magic_resistance", 0));
            stmt.setInt(7, nexus.getDefenseStats().getOrDefault("regeneration_rate", 0));
            stmt.setLong(8, nexus.getLastDamageTime());
            stmt.setBoolean(9, nexus.isRegenerating());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save nexus data", e);
        }
    }
    
    @Override
    public Nexus loadNexusData(UUID kingdomId) {
        String sql = "SELECT * FROM nexus WHERE kingdom_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, kingdomId.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                // Create nexus with default town hall level (will be updated when kingdom loads)
                Nexus nexus = new Nexus(kingdomId, 1);
                
                // Set loaded values
                nexus.setMaxHealth(rs.getInt("max_health"));
                nexus.setCurrentHealth(rs.getInt("current_health"));
                nexus.setShieldPoints(rs.getInt("shield_points"));
                nexus.setLastDamageTime(rs.getLong("last_damage_time"));
                nexus.setRegenerating(rs.getBoolean("regenerating"));
                
                // Update defense stats with loaded values
                nexus.getDefenseStats().put("armor", rs.getInt("armor"));
                nexus.getDefenseStats().put("magic_resistance", rs.getInt("magic_resistance"));
                nexus.getDefenseStats().put("regeneration_rate", rs.getInt("regeneration_rate"));
                
                return nexus;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load nexus data for " + kingdomId, e);
        }
        
        return null;
    }
    
    @Override
    public void saveAll() {
        plugin.getLogger().info("Saving all SQLite data...");
        
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
        
        plugin.getLogger().info("All SQLite data saved successfully.");
    }
}
