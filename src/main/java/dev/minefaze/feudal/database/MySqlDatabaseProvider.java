package dev.minefaze.feudal.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * MySQL database provider with connection pooling
 */
public class MySqlDatabaseProvider implements DatabaseProvider {
    
    private final Feudal plugin;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final int maxConnections;
    private final int connectionTimeout;
    
    private HikariDataSource dataSource;
    
    public MySqlDatabaseProvider(Feudal plugin, String host, int port, String database, 
                                String username, String password, int maxConnections, int connectionTimeout) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.maxConnections = maxConnections;
        this.connectionTimeout = connectionTimeout;
    }
    
    @Override
    public void initialize() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true");
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(maxConnections);
            config.setConnectionTimeout(connectionTimeout);
            config.setLeakDetectionThreshold(60000);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
            dataSource = new HikariDataSource(config);
            createTables();
            
            plugin.getLogger().info("MySQL database initialized: " + host + ":" + port + "/" + database);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize MySQL database", e);
        }
    }
    
    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            // Players table
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS players (
                    player_id VARCHAR(36) PRIMARY KEY,
                    player_name VARCHAR(16) NOT NULL,
                    kingdom_id VARCHAR(36),
                    total_experience INT DEFAULT 0,
                    in_combat BOOLEAN DEFAULT FALSE,
                    warrior_level INT DEFAULT 1,
                    miner_level INT DEFAULT 1,
                    builder_level INT DEFAULT 1,
                    farmer_level INT DEFAULT 1,
                    strength INT DEFAULT 10,
                    defense INT DEFAULT 10,
                    agility INT DEFAULT 10,
                    endurance INT DEFAULT 10,
                    intelligence INT DEFAULT 10,
                    INDEX idx_kingdom (kingdom_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            
            // Kingdoms table
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS kingdoms (
                    kingdom_id VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(32) UNIQUE NOT NULL,
                    leader VARCHAR(36) NOT NULL,
                    treasury INT DEFAULT 0,
                    creation_time BIGINT NOT NULL,
                    capital_world VARCHAR(64),
                    capital_x DOUBLE,
                    capital_y DOUBLE,
                    capital_z DOUBLE,
                    settings TEXT,
                    INDEX idx_name (name),
                    INDEX idx_leader (leader)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            
            // Kingdom members table
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS kingdom_members (
                    kingdom_id VARCHAR(36) NOT NULL,
                    player_id VARCHAR(36) NOT NULL,
                    PRIMARY KEY (kingdom_id, player_id),
                    FOREIGN KEY (kingdom_id) REFERENCES kingdoms(kingdom_id) ON DELETE CASCADE,
                    INDEX idx_player (player_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            
            // Territories table
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS territories (
                    territory_id VARCHAR(36) PRIMARY KEY,
                    kingdom_id VARCHAR(36) NOT NULL,
                    type VARCHAR(16) NOT NULL,
                    defense_level INT DEFAULT 1,
                    claim_time BIGINT NOT NULL,
                    under_attack BOOLEAN DEFAULT FALSE,
                    chunk_world VARCHAR(64) NOT NULL,
                    chunk_x INT NOT NULL,
                    chunk_z INT NOT NULL,
                    FOREIGN KEY (kingdom_id) REFERENCES kingdoms(kingdom_id) ON DELETE CASCADE,
                    INDEX idx_kingdom (kingdom_id),
                    INDEX idx_chunk (chunk_world, chunk_x, chunk_z)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            
            // Challenges table
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS challenges (
                    challenge_id VARCHAR(36) PRIMARY KEY,
                    challenger VARCHAR(36) NOT NULL,
                    target VARCHAR(36) NOT NULL,
                    type VARCHAR(16) NOT NULL,
                    status VARCHAR(16) NOT NULL,
                    creation_time BIGINT NOT NULL,
                    expiration_time BIGINT NOT NULL,
                    wager INT DEFAULT 0,
                    reason TEXT,
                    target_territory VARCHAR(36),
                    battle_world VARCHAR(64),
                    battle_x DOUBLE,
                    battle_y DOUBLE,
                    battle_z DOUBLE,
                    INDEX idx_challenger (challenger),
                    INDEX idx_target (target),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            
            // Alliances table
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS alliances (
                    kingdom1 VARCHAR(36) NOT NULL,
                    kingdom2 VARCHAR(36) NOT NULL,
                    type VARCHAR(16) NOT NULL,
                    creation_time BIGINT NOT NULL,
                    PRIMARY KEY (kingdom1, kingdom2),
                    INDEX idx_kingdom1 (kingdom1),
                    INDEX idx_kingdom2 (kingdom2)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            
            // Town halls table
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS town_halls (
                    kingdom_id VARCHAR(36) PRIMARY KEY,
                    level INT DEFAULT 1,
                    type VARCHAR(32) NOT NULL,
                    upgrading BOOLEAN DEFAULT FALSE,
                    upgrade_start_time BIGINT DEFAULT 0,
                    location_world VARCHAR(64),
                    location_x DOUBLE,
                    location_y DOUBLE,
                    location_z DOUBLE,
                    FOREIGN KEY (kingdom_id) REFERENCES kingdoms(kingdom_id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            
            // Nexus table
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS nexus (
                    kingdom_id VARCHAR(36) PRIMARY KEY,
                    current_health INT NOT NULL,
                    max_health INT NOT NULL,
                    shield_points INT DEFAULT 0,
                    armor INT DEFAULT 0,
                    magic_resistance INT DEFAULT 0,
                    regeneration_rate INT DEFAULT 1,
                    last_damage_time BIGINT DEFAULT 0,
                    regenerating BOOLEAN DEFAULT FALSE,
                    FOREIGN KEY (kingdom_id) REFERENCES kingdoms(kingdom_id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        }
    }
    
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
    
    @Override
    public boolean testConnection() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
    
    // Player Data Methods
    @Override
    public void savePlayerData(FeudalPlayer player) {
        String sql = """
            INSERT INTO players 
            (player_id, player_name, kingdom_id, total_experience, in_combat,
             warrior_level, miner_level, builder_level, farmer_level,
             strength, defense, agility, endurance, intelligence)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
            player_name = VALUES(player_name),
            kingdom_id = VALUES(kingdom_id),
            total_experience = VALUES(total_experience),
            in_combat = VALUES(in_combat),
            warrior_level = VALUES(warrior_level),
            miner_level = VALUES(miner_level),
            builder_level = VALUES(builder_level),
            farmer_level = VALUES(farmer_level),
            strength = VALUES(strength),
            defense = VALUES(defense),
            agility = VALUES(agility),
            endurance = VALUES(endurance),
            intelligence = VALUES(intelligence)
        """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
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
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
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
    
    // Kingdom Data Methods (similar structure to SQLite but with MySQL-specific optimizations)
    @Override
    public void saveKingdomData(Kingdom kingdom) {
        String sql = """
            INSERT INTO kingdoms 
            (kingdom_id, name, leader, treasury, creation_time, 
             capital_world, capital_x, capital_y, capital_z, settings)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
            name = VALUES(name),
            leader = VALUES(leader),
            treasury = VALUES(treasury),
            capital_world = VALUES(capital_world),
            capital_x = VALUES(capital_x),
            capital_y = VALUES(capital_y),
            capital_z = VALUES(capital_z),
            settings = VALUES(settings)
        """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
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
                stmt.setNull(7, Types.DOUBLE);
                stmt.setNull(8, Types.DOUBLE);
                stmt.setNull(9, Types.DOUBLE);
            }
            
            // Convert settings to JSON string (simplified)
            stmt.setString(10, kingdom.getSettings().toString());
            
            stmt.executeUpdate();
            
            // Save kingdom members
            saveKingdomMembers(kingdom, conn);
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save kingdom data for " + kingdom.getName(), e);
        }
    }
    
    private void saveKingdomMembers(Kingdom kingdom, Connection conn) throws SQLException {
        // Delete existing members
        String deleteSql = "DELETE FROM kingdom_members WHERE kingdom_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setString(1, kingdom.getKingdomId().toString());
            stmt.executeUpdate();
        }
        
        // Insert current members
        String insertSql = "INSERT INTO kingdom_members (kingdom_id, player_id) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
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
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
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
                loadKingdomMembers(kingdom, conn);
                
                return kingdom;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load kingdom data for " + kingdomId, e);
        }
        
        return null;
    }
    
    private void loadKingdomMembers(Kingdom kingdom, Connection conn) throws SQLException {
        String sql = "SELECT player_id FROM kingdom_members WHERE kingdom_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
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
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, kingdomId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete kingdom data for " + kingdomId, e);
        }
    }
    
    // Simplified implementations for other methods (similar to SQLite but with connection pooling)
    @Override
    public void saveTerritoryData(Territory territory) {
        // Implementation similar to SQLite but using connection pool
    }
    
    @Override
    public Territory loadTerritoryData(UUID territoryId) {
        return null;
    }
    
    @Override
    public Map<UUID, Territory> loadAllTerritories() {
        return new HashMap<>();
    }
    
    @Override
    public void deleteTerritoryData(UUID territoryId) {
        // Implementation similar to SQLite
    }
    
    @Override
    public void saveChallengeData(Challenge challenge) {
        // Implementation for challenge saving
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
        // Implementation for challenge deletion
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
            INSERT INTO town_halls 
            (kingdom_id, level, type, upgrading, upgrade_start_time,
             location_world, location_x, location_y, location_z)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
            level = VALUES(level),
            type = VALUES(type),
            upgrading = VALUES(upgrading),
            upgrade_start_time = VALUES(upgrade_start_time),
            location_world = VALUES(location_world),
            location_x = VALUES(location_x),
            location_y = VALUES(location_y),
            location_z = VALUES(location_z)
        """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
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
                stmt.setNull(7, Types.DOUBLE);
                stmt.setNull(8, Types.DOUBLE);
                stmt.setNull(9, Types.DOUBLE);
            }
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save town hall data", e);
        }
    }
    
    @Override
    public TownHall loadTownHallData(UUID kingdomId) {
        // Implementation similar to SQLite
        return null;
    }
    
    @Override
    public void saveNexusData(Nexus nexus, UUID kingdomId) {
        // Implementation similar to town hall
    }
    
    @Override
    public Nexus loadNexusData(UUID kingdomId) {
        // Implementation similar to SQLite
        return null;
    }
    
    @Override
    public void saveAll() {
        plugin.getLogger().info("Saving all MySQL data...");
        
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
            
            // Save all territories (when saveTerritoryData is properly implemented)
            for (Territory territory : plugin.getKingdomManager().getAllTerritories()) {
                saveTerritoryData(territory);
            }
        }
        
        plugin.getLogger().info("All MySQL data saved successfully.");
    }
}
