package dev.minefaze.feudal;

import dev.minefaze.feudal.managers.*;
import dev.minefaze.feudal.commands.FeudalCommand;
import dev.minefaze.feudal.listeners.PlayerListener;
import dev.minefaze.feudal.listeners.CombatListener;
import dev.minefaze.feudal.listeners.PvPListener;
import dev.minefaze.feudal.listeners.TerritoryListener;
import dev.minefaze.feudal.gui.GUIManager;
import dev.minefaze.feudal.gui.AnvilGUI;
import dev.minefaze.feudal.gui.KingdomListGUI;
import org.bukkit.plugin.java.JavaPlugin;

public final class Feudal extends JavaPlugin {

    private static Feudal instance;
    
    // Core Managers
    private MessageManager messageManager;
    private KingdomManager kingdomManager;
    private PlayerDataManager playerDataManager;
    private ChallengeManager challengeManager;
    private CombatManager combatManager;
    private DataManager dataManager;
    private GUIManager guiManager;
    private AllianceManager allianceManager;
    private TerritoryVisualizationManager territoryVisualizationManager;
    private TownHallManager townHallManager;
    private SchematicManager schematicManager;
    private NexusManager nexusManager;
    
    // GUI Managers
    private AnvilGUI anvilGUI;
    private KingdomListGUI kingdomListGUI;

    @Override
    public void onEnable() {
        instance = this;
        
        getLogger().info("Starting Feudal RPG Plugin...");
        
        // Initialize configuration
        saveDefaultConfig();
        reloadConfig();
        validateConfig();
        getLogger().info("Configuration loaded successfully!");
        
        // Initialize message manager first (needed by other managers)
        messageManager = new MessageManager(this);
        
        // Initialize data storage
        dataManager = new DataManager(this);
        dataManager.initialize();
        
        // Initialize core managers
        playerDataManager = new PlayerDataManager(this);
        kingdomManager = new KingdomManager(this);
        allianceManager = new AllianceManager(this);
        territoryVisualizationManager = new TerritoryVisualizationManager(this);
        schematicManager = new SchematicManager(this);
        nexusManager = new NexusManager(this);
        townHallManager = new TownHallManager(this);
        challengeManager = new ChallengeManager(this);
        combatManager = new CombatManager(this);
        guiManager = new GUIManager(this);
        
        // Initialize GUI utilities
        anvilGUI = new AnvilGUI(this);
        kingdomListGUI = new KingdomListGUI(this);
        
        // Load existing data in proper order
        getLogger().info(messageManager.getMessage("database.loading-data"));
        kingdomManager.loadKingdomData();
        nexusManager.loadNexusData();
        
        // Register player data manager as listener to handle player join/quit events
        getServer().getPluginManager().registerEvents(playerDataManager, this);
        
        getLogger().info(messageManager.getMessage("database.load-completed"));
        
        // Start auto-save task
        startAutoSaveTask();
        
        // Register commands
        getCommand("feudal").setExecutor(new FeudalCommand(this));
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new PvPListener(this), this);
        getServer().getPluginManager().registerEvents(new TerritoryListener(this), this);
        
        getLogger().info("Feudal RPG Plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down Feudal RPG Plugin...");
        
        // Save all data before shutdown
        if (dataManager != null) {
            dataManager.saveAll();
            dataManager.close();
        }
        
        // Cancel all active challenges
        if (challengeManager != null) {
            challengeManager.cancelAllChallenges();
        }
        
        // Shutdown territory visualization
        if (territoryVisualizationManager != null) {
            territoryVisualizationManager.shutdown();
        }
        
        // Shutdown town hall manager
        if (townHallManager != null) {
            townHallManager.shutdown();
        }
        
        // Shutdown GUI utilities
        if (anvilGUI != null) {
            anvilGUI.shutdown();
        }
        if (kingdomListGUI != null) {
            kingdomListGUI.shutdown();
        }
        
        getLogger().info("Feudal RPG Plugin disabled.");
    }
    
    /**
     * Validate configuration values and warn about invalid settings
     */
    private void validateConfig() {
        // Validate auto-save interval
        int autoSaveInterval = getConfig().getInt("general.auto-save-interval", 10);
        if (autoSaveInterval < 0) {
            getLogger().warning("Invalid auto-save-interval: " + autoSaveInterval + ". Using default: 10");
            getConfig().set("general.auto-save-interval", 10);
        }
        
        // Validate kingdom settings
        int maxKingdoms = getConfig().getInt("kingdoms.max-kingdoms", 50);
        if (maxKingdoms < 1) {
            getLogger().warning("Invalid max-kingdoms: " + maxKingdoms + ". Using default: 50");
            getConfig().set("kingdoms.max-kingdoms", 50);
        }
        
        // Validate database type
        String dbType = getConfig().getString("database.type", "yaml").toLowerCase();
        if (!dbType.equals("yaml") && !dbType.equals("sqlite") && !dbType.equals("mysql")) {
            getLogger().warning("Invalid database type: " + dbType + ". Using default: yaml");
            getConfig().set("database.type", "yaml");
        }
        
        // Validate language
        String language = getConfig().getString("general.language", "en");
        if (language == null || language.trim().isEmpty()) {
            getLogger().warning("Invalid language setting. Using default: en");
            getConfig().set("general.language", "en");
        }
        
        getLogger().info("Configuration validation completed");
    }
    
    private void startAutoSaveTask() {
        int autoSaveInterval = getConfig().getInt("general.auto-save-interval", 10);
        if (autoSaveInterval > 0) {
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                getLogger().info(messageManager.getMessage("database.saving-data"));
                dataManager.saveAll();
                getLogger().info(messageManager.getMessage("database.save-completed"));
            }, 20L * 60L * autoSaveInterval, 20L * 60L * autoSaveInterval); // Convert minutes to ticks
            
            getLogger().info(messageManager.getMessage("database.auto-save-enabled", String.valueOf(autoSaveInterval)));
        } else {
            getLogger().info(messageManager.getMessage("database.auto-save-disabled"));
        }
    }
    
    // Getters for managers
    public static Feudal getInstance() { return instance; }
    public MessageManager getMessageManager() { return messageManager; }
    public KingdomManager getKingdomManager() { return kingdomManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public ChallengeManager getChallengeManager() { return challengeManager; }
    public CombatManager getCombatManager() { return combatManager; }
    public DataManager getDataManager() { return dataManager; }
    public GUIManager getGUIManager() { return guiManager; }
    public AllianceManager getAllianceManager() { return allianceManager; }
    public TerritoryVisualizationManager getTerritoryVisualizationManager() { return territoryVisualizationManager; }
    public TownHallManager getTownHallManager() { return townHallManager; }
    public SchematicManager getSchematicManager() { return schematicManager; }
    public NexusManager getNexusManager() { return nexusManager; }
    
    // GUI Getters
    public AnvilGUI getAnvilGUI() { return anvilGUI; }
    public KingdomListGUI getKingdomListGUI() { return kingdomListGUI; }
}
