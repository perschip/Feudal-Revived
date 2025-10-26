package dev.minefaze.feudal.database;

import dev.minefaze.feudal.Feudal;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Factory class for creating database providers based on configuration
 */
public class DatabaseFactory {
    
    public static DatabaseProvider createProvider(Feudal plugin) {
        FileConfiguration config = plugin.getConfig();
        String type = config.getString("database.type", "yaml").toLowerCase();
        
        switch (type) {
            case "yaml", "file" -> {
                return new YamlDatabaseProvider(plugin);
            }
            case "sqlite" -> {
                String filename = config.getString("database.sqlite.filename", "feudal.db");
                return new SqliteDatabaseProvider(plugin, filename);
            }
            case "mysql" -> {
                String host = config.getString("database.mysql.host", "localhost");
                int port = config.getInt("database.mysql.port", 3306);
                String database = config.getString("database.mysql.database", "feudal");
                String username = config.getString("database.mysql.username", "feudal_user");
                String password = config.getString("database.mysql.password", "password");
                int maxConnections = config.getInt("database.mysql.max-connections", 10);
                int connectionTimeout = config.getInt("database.mysql.connection-timeout", 30000);
                
                return new MySqlDatabaseProvider(plugin, host, port, database, username, password, maxConnections, connectionTimeout);
            }
            default -> {
                plugin.getLogger().warning("Unknown database type: " + type + ". Falling back to YAML.");
                return new YamlDatabaseProvider(plugin);
            }
        }
    }
}
