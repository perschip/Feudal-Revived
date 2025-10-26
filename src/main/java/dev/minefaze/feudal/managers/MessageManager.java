package dev.minefaze.feudal.managers;

import dev.minefaze.feudal.Feudal;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages internationalization and message handling for the Feudal plugin
 */
public class MessageManager {
    
    private final Feudal plugin;
    private final Map<String, FileConfiguration> languageConfigs;
    private String defaultLanguage;
    private FileConfiguration defaultConfig;
    
    public MessageManager(Feudal plugin) {
        this.plugin = plugin;
        this.languageConfigs = new HashMap<>();
        this.defaultLanguage = plugin.getConfig().getString("general.language", "en");
        
        loadLanguages();
    }
    
    /**
     * Load all available language files
     */
    private void loadLanguages() {
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        
        // Create languages directory if it doesn't exist
        if (!languagesDir.exists()) {
            languagesDir.mkdirs();
        }
        
        // Copy default language files from resources if they don't exist
        copyDefaultLanguageFiles(languagesDir);
        
        // Load all language files
        File[] languageFiles = languagesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (languageFiles != null) {
            for (File file : languageFiles) {
                String language = file.getName().replace(".yml", "");
                try {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    languageConfigs.put(language, config);
                    plugin.getLogger().info("Loaded language: " + language);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load language file: " + file.getName(), e);
                }
            }
        }
        
        // Set default configuration
        defaultConfig = languageConfigs.get(defaultLanguage);
        if (defaultConfig == null) {
            defaultConfig = languageConfigs.get("en"); // Fallback to English
            if (defaultConfig == null) {
                plugin.getLogger().severe("No language files found! Plugin may not function correctly.");
            }
        }
    }
    
    /**
     * Copy default language files from plugin resources
     */
    private void copyDefaultLanguageFiles(File languagesDir) {
        String[] defaultLanguages = {"en.yml", "es.yml"};
        
        for (String languageFile : defaultLanguages) {
            File targetFile = new File(languagesDir, languageFile);
            if (!targetFile.exists()) {
                try (InputStream inputStream = plugin.getResource("languages/" + languageFile)) {
                    if (inputStream != null) {
                        Files.copy(inputStream, targetFile.toPath());
                        plugin.getLogger().info("Created default language file: " + languageFile);
                    }
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to copy default language file: " + languageFile, e);
                }
            }
        }
    }
    
    /**
     * Get a message in the default language
     * @param key The message key (e.g., "general.no-permission")
     * @param args Optional arguments for message formatting
     * @return The formatted message
     */
    public String getMessage(String key, Object... args) {
        return getMessage(defaultLanguage, key, args);
    }
    
    /**
     * Get a message in a specific language
     * @param language The language code
     * @param key The message key
     * @param args Optional arguments for message formatting
     * @return The formatted message
     */
    public String getMessage(String language, String key, Object... args) {
        FileConfiguration config = languageConfigs.get(language);
        if (config == null) {
            config = defaultConfig; // Fallback to default language
        }
        
        String message = config.getString(key);
        if (message == null) {
            // Try fallback to default language if key not found
            if (config != defaultConfig && defaultConfig != null) {
                message = defaultConfig.getString(key);
            }
            
            // If still null, return the key itself as fallback
            if (message == null) {
                plugin.getLogger().warning("Missing translation key: " + key);
                return "§c[Missing: " + key + "]";
            }
        }
        
        // Format message with arguments if provided
        if (args.length > 0) {
            try {
                return MessageFormat.format(message, args);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Failed to format message for key: " + key + " - " + e.getMessage());
                return message; // Return unformatted message as fallback
            }
        }
        
        return message;
    }
    
    /**
     * Send a message to a player using the default language
     * @param player The player to send the message to
     * @param key The message key
     * @param args Optional arguments for message formatting
     */
    public void sendMessage(Player player, String key, Object... args) {
        String message = getMessage(key, args);
        player.sendMessage(message);
    }
    
    /**
     * Send a message to a player using a specific language
     * @param player The player to send the message to
     * @param language The language code
     * @param key The message key
     * @param args Optional arguments for message formatting
     */
    public void sendMessage(Player player, String language, String key, Object... args) {
        String message = getMessage(language, key, args);
        player.sendMessage(message);
    }
    
    /**
     * Get the prefix for messages
     * @return The message prefix
     */
    public String getPrefix() {
        return getMessage("general.prefix");
    }
    
    /**
     * Send a prefixed message to a player
     * @param player The player to send the message to
     * @param key The message key
     * @param args Optional arguments for message formatting
     */
    public void sendPrefixedMessage(Player player, String key, Object... args) {
        String prefix = getPrefix();
        String message = getMessage(key, args);
        player.sendMessage(prefix + message);
    }
    
    /**
     * Get all available languages
     * @return Set of available language codes
     */
    public java.util.Set<String> getAvailableLanguages() {
        return languageConfigs.keySet();
    }
    
    /**
     * Check if a language is available
     * @param language The language code to check
     * @return True if the language is available
     */
    public boolean isLanguageAvailable(String language) {
        return languageConfigs.containsKey(language);
    }
    
    /**
     * Get the default language
     * @return The default language code
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    
    /**
     * Reload all language files
     */
    public void reload() {
        languageConfigs.clear();
        defaultLanguage = plugin.getConfig().getString("general.language", "en");
        loadLanguages();
        plugin.getLogger().info("Language files reloaded");
    }
}
