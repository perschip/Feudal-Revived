package dev.minefaze.feudal.managers;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.TownHall;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

/**
 * Manages schematic loading and building for town halls and other structures
 */
public class SchematicManager {
    
    private final Feudal plugin;
    private final File schematicsFolder;
    private final Map<String, SchematicData> loadedSchematics;
    
    public SchematicManager(Feudal plugin) {
        this.plugin = plugin;
        this.schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        this.loadedSchematics = new HashMap<>();
        
        initializeSchematicsFolder();
        loadSchematics();
    }
    
    /**
     * Initialize the schematics folder structure
     */
    private void initializeSchematicsFolder() {
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }
        
        // Create subfolder structure
        String[] subfolders = {
            "townhalls/medieval",
            "townhalls/fantasy", 
            "townhalls/modern",
            "nexus",
            "decorations/flags",
            "decorations/walls",
            "decorations/gates"
        };
        
        for (String subfolder : subfolders) {
            File folder = new File(schematicsFolder, subfolder);
            if (!folder.exists()) {
                folder.mkdirs();
            }
        }
        
        plugin.getLogger().info("Schematics folder structure initialized at: " + schematicsFolder.getPath());
    }
    
    /**
     * Load all schematics from the schematics folder
     */
    public void loadSchematics() {
        loadedSchematics.clear();
        
        try {
            loadTownHallSchematics();
            loadNexusSchematics();
            loadDecorationSchematics();
            
            plugin.getLogger().info("Loaded " + loadedSchematics.size() + " schematics");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load schematics", e);
        }
    }
    
    /**
     * Load town hall schematics for all types and levels
     */
    private void loadTownHallSchematics() {
        File townhallsFolder = new File(schematicsFolder, "townhalls");
        if (!townhallsFolder.exists()) return;
        
        for (TownHall.TownHallType type : TownHall.TownHallType.values()) {
            String typeName = type.name().toLowerCase();
            File typeFolder = new File(townhallsFolder, typeName);
            
            if (typeFolder.exists() && typeFolder.isDirectory()) {
                for (int level = 1; level <= 10; level++) {
                    File schemFile = new File(typeFolder, "level_" + level + ".schem");
                    File legacyFile = new File(typeFolder, "level_" + level + ".schematic");
                    
                    File fileToLoad = schemFile.exists() ? schemFile : 
                                     legacyFile.exists() ? legacyFile : null;
                    
                    if (fileToLoad != null) {
                        try {
                            SchematicData schematic = loadSchematicFile(fileToLoad);
                            String key = "townhall_" + typeName + "_" + level;
                            loadedSchematics.put(key, schematic);
                            
                            plugin.getLogger().info("Loaded schematic: " + key);
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING, 
                                "Failed to load schematic: " + fileToLoad.getName(), e);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Load nexus schematics
     */
    private void loadNexusSchematics() {
        File nexusFolder = new File(schematicsFolder, "nexus");
        if (!nexusFolder.exists()) return;
        
        for (TownHall.TownHallType type : TownHall.TownHallType.values()) {
            String typeName = type.name().toLowerCase();
            File schemFile = new File(nexusFolder, typeName + "_nexus.schem");
            File legacyFile = new File(nexusFolder, typeName + "_nexus.schematic");
            
            File fileToLoad = schemFile.exists() ? schemFile : 
                             legacyFile.exists() ? legacyFile : null;
            
            if (fileToLoad != null) {
                try {
                    SchematicData schematic = loadSchematicFile(fileToLoad);
                    String key = "nexus_" + typeName;
                    loadedSchematics.put(key, schematic);
                    
                    plugin.getLogger().info("Loaded nexus schematic: " + key);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, 
                        "Failed to load nexus schematic: " + fileToLoad.getName(), e);
                }
            }
        }
    }
    
    /**
     * Load decoration schematics
     */
    private void loadDecorationSchematics() {
        File decorationsFolder = new File(schematicsFolder, "decorations");
        if (!decorationsFolder.exists()) return;
        
        loadSchematicsFromFolder(decorationsFolder, "decoration");
    }
    
    /**
     * Recursively load schematics from a folder
     */
    private void loadSchematicsFromFolder(File folder, String prefix) {
        File[] files = folder.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                loadSchematicsFromFolder(file, prefix + "_" + file.getName());
            } else if (file.getName().endsWith(".schem") || file.getName().endsWith(".schematic")) {
                try {
                    SchematicData schematic = loadSchematicFile(file);
                    String key = prefix + "_" + file.getName().replaceAll("\\.(schem|schematic)$", "");
                    loadedSchematics.put(key, schematic);
                    
                    plugin.getLogger().info("Loaded decoration schematic: " + key);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, 
                        "Failed to load decoration schematic: " + file.getName(), e);
                }
            }
        }
    }
    
    /**
     * Load a schematic file and parse it into SchematicData
     */
    private SchematicData loadSchematicFile(File file) throws IOException {
        if (file.getName().endsWith(".schem")) {
            return loadSpongeSchematic(file);
        } else {
            return loadLegacySchematic(file);
        }
    }
    
    /**
     * Load Sponge format schematic (.schem)
     */
    private SchematicData loadSpongeSchematic(File file) throws IOException {
        // This is a simplified implementation
        // In a real implementation, you'd use NBT parsing libraries
        plugin.getLogger().warning("Sponge schematic format (.schem) not fully implemented yet. " +
                                  "Please use legacy format (.schematic) for now: " + file.getName());
        return createDefaultSchematic();
    }
    
    /**
     * Load legacy WorldEdit schematic (.schematic)
     */
    private SchematicData loadLegacySchematic(File file) throws IOException {
        // This is a simplified implementation
        // In a real implementation, you'd use NBT parsing libraries
        plugin.getLogger().warning("Legacy schematic format (.schematic) not fully implemented yet. " +
                                  "Using default structure for: " + file.getName());
        return createDefaultSchematic();
    }
    
    /**
     * Create a default schematic structure
     */
    private SchematicData createDefaultSchematic() {
        // Create a simple 5x5x5 structure as default
        Map<Location, Material> blocks = new HashMap<>();
        
        // This would be replaced with actual schematic data
        return new SchematicData(5, 5, 5, blocks);
    }
    
    /**
     * Build a town hall schematic at the specified location
     */
    public void buildTownHall(TownHall townHall, Location location) {
        plugin.getLogger().info("SchematicManager.buildTownHall called for " + townHall.getType() + " level " + townHall.getLevel());
        
        if (!plugin.getConfig().getBoolean("townhall.auto-build", true)) {
            plugin.getLogger().info("Town hall auto-build is disabled in config");
            return;
        }
        
        String key = "townhall_" + townHall.getType().name().toLowerCase() + "_" + townHall.getLevel();
        plugin.getLogger().info("Looking for schematic with key: " + key);
        SchematicData schematic = loadedSchematics.get(key);
        
        if (schematic == null) {
            plugin.getLogger().warning("No schematic found for: " + key + ". Building default structure.");
            plugin.getLogger().info("Available schematics: " + loadedSchematics.keySet());
            buildDefaultTownHall(townHall, location);
            return;
        }
        
        plugin.getLogger().info("Found schematic, building at location: " + location);
        buildSchematic(schematic, location, "Town Hall");
    }
    
    /**
     * Build a nexus schematic at the specified location
     */
    public void buildNexus(TownHall.TownHallType type, Location location) {
        String key = "nexus_" + type.name().toLowerCase();
        SchematicData schematic = loadedSchematics.get(key);
        
        if (schematic == null) {
            plugin.getLogger().warning("No nexus schematic found for: " + key + ". Building default structure.");
            buildDefaultNexus(location);
            return;
        }
        
        buildSchematic(schematic, location, "Nexus");
    }
    
    /**
     * Build a schematic at the specified location
     */
    public void buildSchematic(SchematicData schematic, Location location, String structureName) {
        new BukkitRunnable() {
            private int blocksPlaced = 0;
            private final Iterator<Map.Entry<Location, Material>> blockIterator = 
                schematic.getBlocks().entrySet().iterator();
            
            @Override
            public void run() {
                int blocksPerTick = plugin.getConfig().getInt("performance.batch-size", 100);
                
                for (int i = 0; i < blocksPerTick && blockIterator.hasNext(); i++) {
                    Map.Entry<Location, Material> entry = blockIterator.next();
                    Location relativePos = entry.getKey();
                    Material material = entry.getValue();
                    
                    // Calculate absolute position
                    Location absolutePos = location.clone().add(relativePos);
                    Block block = absolutePos.getBlock();
                    
                    // Set block type
                    block.setType(material);
                    blocksPlaced++;
                }
                
                if (!blockIterator.hasNext()) {
                    plugin.getLogger().info("Finished building " + structureName + 
                                          " with " + blocksPlaced + " blocks");
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick
    }
    
    /**
     * Build a default town hall structure when no schematic is available
     */
    private void buildDefaultTownHall(TownHall townHall, Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        int size = Math.min(3 + townHall.getLevel(), 10); // Scale with level
        Material material = getDefaultMaterial(townHall.getType());
        
        // Build a simple structure
        new BukkitRunnable() {
            private int x = -size/2;
            private int z = -size/2;
            private int y = 0;
            
            @Override
            public void run() {
                int blocksPlaced = 0;
                int maxBlocksPerTick = 50;
                
                while (blocksPlaced < maxBlocksPerTick && y < size) {
                    Location blockLoc = location.clone().add(x, y, z);
                    Block block = blockLoc.getBlock();
                    
                    // Build walls and floor
                    if (y == 0 || x == -size/2 || x == size/2 || z == -size/2 || z == size/2) {
                        block.setType(material);
                    }
                    
                    blocksPlaced++;
                    
                    // Move to next position
                    x++;
                    if (x > size/2) {
                        x = -size/2;
                        z++;
                        if (z > size/2) {
                            z = -size/2;
                            y++;
                        }
                    }
                }
                
                if (y >= size) {
                    plugin.getLogger().info("Finished building default town hall");
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * Build a default nexus structure
     */
    private void buildDefaultNexus(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        // Build a simple beacon structure
        location.getBlock().setType(Material.BEACON);
        location.clone().add(0, -1, 0).getBlock().setType(Material.IRON_BLOCK);
        
        plugin.getLogger().info("Built default nexus structure");
    }
    
    /**
     * Get default material based on town hall type
     */
    private Material getDefaultMaterial(TownHall.TownHallType type) {
        return switch (type) {
            case MEDIEVAL -> Material.STONE_BRICKS;
            case FANTASY -> Material.PURPUR_BLOCK;
            case MODERN -> Material.QUARTZ_BLOCK;
        };
    }
    
    /**
     * Check if a schematic exists for the given key
     */
    public boolean hasSchematic(String key) {
        return loadedSchematics.containsKey(key);
    }
    
    /**
     * Get the schematics folder
     */
    public File getSchematicsFolder() {
        return schematicsFolder;
    }
    
    /**
     * Get all loaded schematic keys
     */
    public Set<String> getLoadedSchematicKeys() {
        return new HashSet<>(loadedSchematics.keySet());
    }
    
    /**
     * Data class to hold schematic information
     */
    public static class SchematicData {
        private final int width, height, length;
        private final Map<Location, Material> blocks;
        
        public SchematicData(int width, int height, int length, Map<Location, Material> blocks) {
            this.width = width;
            this.height = height;
            this.length = length;
            this.blocks = blocks;
        }
        
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getLength() { return length; }
        public Map<Location, Material> getBlocks() { return blocks; }
    }
}
