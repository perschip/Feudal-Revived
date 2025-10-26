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

// NBT parsing imports
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;

// For NBT handling - using reflection to access CraftBukkit NBT classes
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

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
            // Note: nexus folder removed - nexus now uses simple end crystal + hologram
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
            // Note: Nexus schematics removed - nexus now uses simple end crystal + hologram
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
        plugin.getLogger().info("Loading Sponge schematic: " + file.getName());
        
        try {
            // Read the NBT data from the .schem file
            Map<String, Object> nbtData = readNBTFile(file);
            
            if (nbtData == null) {
                plugin.getLogger().warning("Failed to read NBT data from: " + file.getName());
                return createDefaultSchematic();
            }
            
            // Extract dimensions
            short width = getShort(nbtData, "Width");
            short height = getShort(nbtData, "Height");
            short length = getShort(nbtData, "Length");
            
            plugin.getLogger().info("Schematic dimensions: " + width + "x" + height + "x" + length);
            
            // Extract block data
            Map<Location, Material> blocks = new HashMap<>();
            
            // Get palette and block data
            Map<String, Object> palette = getCompound(nbtData, "Palette");
            byte[] blockData = getByteArray(nbtData, "BlockData");
            
            if (palette != null && blockData != null) {
                // Create reverse palette mapping
                Map<Integer, String> reversePalette = new HashMap<>();
                for (Map.Entry<String, Object> entry : palette.entrySet()) {
                    reversePalette.put(getInt(entry.getValue()), entry.getKey());
                }
                
                // Parse block data using varint encoding
                int index = 0;
                for (int y = 0; y < height; y++) {
                    for (int z = 0; z < length; z++) {
                        for (int x = 0; x < width; x++) {
                            if (index < blockData.length) {
                                int paletteId = readVarInt(blockData, index);
                                String blockName = reversePalette.get(paletteId);
                                
                                if (blockName != null && !blockName.equals("minecraft:air")) {
                                    Material material = parseMaterial(blockName);
                                    if (material != null && material != Material.AIR) {
                                        Location relativePos = new Location(null, x, y, z);
                                        blocks.put(relativePos, material);
                                    }
                                }
                                
                                index += getVarIntSize(paletteId);
                            }
                        }
                    }
                }
            }
            
            plugin.getLogger().info("Loaded " + blocks.size() + " blocks from schematic");
            return new SchematicData(width, height, length, blocks);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading Sponge schematic: " + file.getName(), e);
            return createDefaultSchematic();
        }
    }
    
    /**
     * Load legacy WorldEdit schematic (.schematic)
     */
    private SchematicData loadLegacySchematic(File file) throws IOException {
        plugin.getLogger().info("Loading legacy schematic: " + file.getName());
        
        try {
            // Read the NBT data from the .schematic file
            Map<String, Object> nbtData = readNBTFile(file);
            
            if (nbtData == null) {
                plugin.getLogger().warning("Failed to read NBT data from: " + file.getName());
                return createDefaultSchematic();
            }
            
            // Extract dimensions
            short width = getShort(nbtData, "Width");
            short height = getShort(nbtData, "Height");
            short length = getShort(nbtData, "Length");
            
            plugin.getLogger().info("Schematic dimensions: " + width + "x" + height + "x" + length);
            
            // Extract block data
            Map<Location, Material> blocks = new HashMap<>();
            
            byte[] blockIds = getByteArray(nbtData, "Blocks");
            byte[] blockData = getByteArray(nbtData, "Data");
            
            if (blockIds != null) {
                for (int index = 0; index < blockIds.length; index++) {
                    int blockId = blockIds[index] & 0xFF;
                    int data = (blockData != null && index < blockData.length) ? blockData[index] & 0xFF : 0;
                    
                    if (blockId != 0) { // Skip air blocks
                        // Calculate position from index
                        int y = index / (width * length);
                        int z = (index % (width * length)) / width;
                        int x = index % width;
                        
                        Material material = legacyIdToMaterial(blockId, data);
                        if (material != null && material != Material.AIR) {
                            Location relativePos = new Location(null, x, y, z);
                            blocks.put(relativePos, material);
                        }
                    }
                }
            }
            
            plugin.getLogger().info("Loaded " + blocks.size() + " blocks from legacy schematic");
            return new SchematicData(width, height, length, blocks);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading legacy schematic: " + file.getName(), e);
            return createDefaultSchematic();
        }
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
     * Build a nexus structure at the specified location
     * Nexus uses a simple beacon-based structure with themed decorations
     */
    public void buildNexus(TownHall.TownHallType type, Location location) {
        plugin.getLogger().info("SchematicManager.buildNexus called for " + type + " at " + location);
        
        if (!plugin.getConfig().getBoolean("nexus.auto-build", true)) {
            plugin.getLogger().info("Nexus auto-build is disabled in config");
            return;
        }
        
        // Check if we have a custom nexus schematic
        String key = "nexus_" + type.name().toLowerCase();
        SchematicData schematic = loadedSchematics.get(key);
        
        if (schematic != null) {
            plugin.getLogger().info("Found custom nexus schematic, building: " + key);
            buildSchematic(schematic, location, "Nexus");
        } else {
            plugin.getLogger().info("No custom schematic found, building default nexus structure");
            buildDefaultNexus(type, location);
        }
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
     * Build a default nexus structure when no schematic is available
     * Creates a beacon-based structure with themed decorations
     */
    private void buildDefaultNexus(TownHall.TownHallType type, Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        plugin.getLogger().info("Building default nexus structure for type: " + type + " at " + location);
        
        // Build the structure asynchronously to avoid lag
        new BukkitRunnable() {
            @Override
            public void run() {
                // Build 3x3 iron block base for beacon power
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Location baseLocation = location.clone().add(x, -1, z);
                        baseLocation.getBlock().setType(Material.IRON_BLOCK);
                    }
                }
                
                // Place beacon at center
                location.getBlock().setType(Material.BEACON);
                
                // Add themed decorative blocks around the beacon
                Material decorativeMaterial = getDefaultMaterial(type);
                
                // Place decorative corners
                for (int x = -2; x <= 2; x += 4) {
                    for (int z = -2; z <= 2; z += 4) {
                        Location cornerLocation = location.clone().add(x, 0, z);
                        cornerLocation.getBlock().setType(decorativeMaterial);
                        
                        // Add pillar
                        cornerLocation.clone().add(0, 1, 0).getBlock().setType(decorativeMaterial);
                        cornerLocation.clone().add(0, 2, 0).getBlock().setType(decorativeMaterial);
                    }
                }
                
                // Add decorative walls
                for (int i = -1; i <= 1; i += 2) {
                    // North-South walls
                    location.clone().add(i, 0, -2).getBlock().setType(decorativeMaterial);
                    location.clone().add(i, 0, 2).getBlock().setType(decorativeMaterial);
                    location.clone().add(i, 1, -2).getBlock().setType(decorativeMaterial);
                    location.clone().add(i, 1, 2).getBlock().setType(decorativeMaterial);
                    
                    // East-West walls
                    location.clone().add(-2, 0, i).getBlock().setType(decorativeMaterial);
                    location.clone().add(2, 0, i).getBlock().setType(decorativeMaterial);
                    location.clone().add(-2, 1, i).getBlock().setType(decorativeMaterial);
                    location.clone().add(2, 1, i).getBlock().setType(decorativeMaterial);
                }
                
                plugin.getLogger().info("Finished building default nexus structure");
            }
        }.runTaskLater(plugin, 1L); // Run next tick to avoid blocking
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
    
    // NBT Helper Methods
    
    /**
     * Read NBT data from a file (supports both compressed and uncompressed)
     */
    private Map<String, Object> readNBTFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            // Try to read as compressed first
            try (GZIPInputStream gzis = new GZIPInputStream(fis)) {
                return readNBTFromStream(gzis);
            } catch (IOException e) {
                // If that fails, try uncompressed
                try (FileInputStream fis2 = new FileInputStream(file)) {
                    return readNBTFromStream(fis2);
                }
            }
        }
    }
    
    /**
     * Simple NBT reader implementation
     */
    private Map<String, Object> readNBTFromStream(InputStream stream) throws IOException {
        DataInputStream dis = new DataInputStream(stream);
        
        // Read NBT root compound
        byte type = dis.readByte();
        if (type != 10) { // TAG_Compound
            throw new IOException("Expected compound tag at root");
        }
        
        // Read root name length and name
        short nameLength = dis.readShort();
        if (nameLength > 0) {
            dis.skipBytes(nameLength);
        }
        
        return readCompound(dis);
    }
    
    /**
     * Read NBT compound tag
     */
    private Map<String, Object> readCompound(DataInputStream dis) throws IOException {
        Map<String, Object> compound = new HashMap<>();
        
        while (true) {
            byte type = dis.readByte();
            if (type == 0) break; // TAG_End
            
            // Read name
            short nameLength = dis.readShort();
            String name = "";
            if (nameLength > 0) {
                byte[] nameBytes = new byte[nameLength];
                dis.readFully(nameBytes);
                name = new String(nameBytes, "UTF-8");
            }
            
            // Read value based on type
            Object value = readNBTValue(dis, type);
            compound.put(name, value);
        }
        
        return compound;
    }
    
    /**
     * Read NBT value based on type
     */
    private Object readNBTValue(DataInputStream dis, byte type) throws IOException {
        return switch (type) {
            case 1 -> dis.readByte(); // TAG_Byte
            case 2 -> dis.readShort(); // TAG_Short
            case 3 -> dis.readInt(); // TAG_Int
            case 4 -> dis.readLong(); // TAG_Long
            case 5 -> dis.readFloat(); // TAG_Float
            case 6 -> dis.readDouble(); // TAG_Double
            case 7 -> { // TAG_Byte_Array
                int length = dis.readInt();
                byte[] array = new byte[length];
                dis.readFully(array);
                yield array;
            }
            case 8 -> { // TAG_String
                short length = dis.readShort();
                if (length == 0) yield "";
                byte[] bytes = new byte[length];
                dis.readFully(bytes);
                yield new String(bytes, "UTF-8");
            }
            case 9 -> { // TAG_List
                byte listType = dis.readByte();
                int length = dis.readInt();
                List<Object> list = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    list.add(readNBTValue(dis, listType));
                }
                yield list;
            }
            case 10 -> readCompound(dis); // TAG_Compound
            case 11 -> { // TAG_Int_Array
                int length = dis.readInt();
                int[] array = new int[length];
                for (int i = 0; i < length; i++) {
                    array[i] = dis.readInt();
                }
                yield array;
            }
            case 12 -> { // TAG_Long_Array
                int length = dis.readInt();
                long[] array = new long[length];
                for (int i = 0; i < length; i++) {
                    array[i] = dis.readLong();
                }
                yield array;
            }
            default -> throw new IOException("Unknown NBT tag type: " + type);
        };
    }
    
    // Helper methods for extracting data from NBT
    private short getShort(Map<String, Object> nbt, String key) {
        Object value = nbt.get(key);
        if (value instanceof Short) return (Short) value;
        if (value instanceof Integer) return ((Integer) value).shortValue();
        return 0;
    }
    
    private int getInt(Object value) {
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Short) return ((Short) value).intValue();
        if (value instanceof Byte) return ((Byte) value).intValue();
        return 0;
    }
    
    private byte[] getByteArray(Map<String, Object> nbt, String key) {
        Object value = nbt.get(key);
        return value instanceof byte[] ? (byte[]) value : null;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> getCompound(Map<String, Object> nbt, String key) {
        Object value = nbt.get(key);
        return value instanceof Map ? (Map<String, Object>) value : null;
    }
    
    /**
     * Read variable-length integer from byte array
     */
    private int readVarInt(byte[] data, int offset) {
        int value = 0;
        int position = 0;
        byte currentByte;
        
        while (offset < data.length) {
            currentByte = data[offset++];
            value |= (currentByte & 0x7F) << position;
            
            if ((currentByte & 0x80) == 0) break;
            position += 7;
            
            if (position >= 32) throw new RuntimeException("VarInt is too big");
        }
        
        return value;
    }
    
    /**
     * Get the size of a varint in bytes
     */
    private int getVarIntSize(int value) {
        int size = 0;
        do {
            value >>>= 7;
            size++;
        } while (value != 0);
        return size;
    }
    
    /**
     * Parse material from Minecraft block name
     */
    private Material parseMaterial(String blockName) {
        if (blockName == null) return null;
        
        // Remove minecraft: prefix if present
        String materialName = blockName.replace("minecraft:", "").toUpperCase();
        
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            // Try some common conversions
            materialName = materialName.replace("_BLOCK", "");
            try {
                return Material.valueOf(materialName);
            } catch (IllegalArgumentException e2) {
                plugin.getLogger().warning("Unknown material: " + blockName);
                return Material.STONE; // Default fallback
            }
        }
    }
    
    /**
     * Convert legacy block ID and data to modern Material
     */
    private Material legacyIdToMaterial(int blockId, int data) {
        // This is a simplified mapping - in reality you'd need a comprehensive lookup table
        return switch (blockId) {
            case 1 -> switch (data) {
                case 1 -> Material.GRANITE;
                case 2 -> Material.POLISHED_GRANITE;
                case 3 -> Material.DIORITE;
                case 4 -> Material.POLISHED_DIORITE;
                case 5 -> Material.ANDESITE;
                case 6 -> Material.POLISHED_ANDESITE;
                default -> Material.STONE;
            };
            case 2 -> Material.GRASS_BLOCK;
            case 3 -> switch (data) {
                case 1 -> Material.COARSE_DIRT;
                case 2 -> Material.PODZOL;
                default -> Material.DIRT;
            };
            case 4 -> Material.COBBLESTONE;
            case 5 -> switch (data) {
                case 1 -> Material.SPRUCE_PLANKS;
                case 2 -> Material.BIRCH_PLANKS;
                case 3 -> Material.JUNGLE_PLANKS;
                case 4 -> Material.ACACIA_PLANKS;
                case 5 -> Material.DARK_OAK_PLANKS;
                default -> Material.OAK_PLANKS;
            };
            case 17 -> switch (data & 3) {
                case 1 -> Material.SPRUCE_LOG;
                case 2 -> Material.BIRCH_LOG;
                case 3 -> Material.JUNGLE_LOG;
                default -> Material.OAK_LOG;
            };
            case 35 -> switch (data) {
                case 1 -> Material.ORANGE_WOOL;
                case 2 -> Material.MAGENTA_WOOL;
                case 3 -> Material.LIGHT_BLUE_WOOL;
                case 4 -> Material.YELLOW_WOOL;
                case 5 -> Material.LIME_WOOL;
                case 6 -> Material.PINK_WOOL;
                case 7 -> Material.GRAY_WOOL;
                case 8 -> Material.LIGHT_GRAY_WOOL;
                case 9 -> Material.CYAN_WOOL;
                case 10 -> Material.PURPLE_WOOL;
                case 11 -> Material.BLUE_WOOL;
                case 12 -> Material.BROWN_WOOL;
                case 13 -> Material.GREEN_WOOL;
                case 14 -> Material.RED_WOOL;
                case 15 -> Material.BLACK_WOOL;
                default -> Material.WHITE_WOOL;
            };
            case 42 -> Material.IRON_BLOCK;
            case 57 -> Material.DIAMOND_BLOCK;
            case 98 -> switch (data) {
                case 1 -> Material.MOSSY_STONE_BRICKS;
                case 2 -> Material.CRACKED_STONE_BRICKS;
                case 3 -> Material.CHISELED_STONE_BRICKS;
                default -> Material.STONE_BRICKS;
            };
            case 138 -> Material.BEACON;
            case 155 -> Material.QUARTZ_BLOCK;
            case 201 -> Material.PURPUR_BLOCK;
            default -> {
                plugin.getLogger().warning("Unknown legacy block ID: " + blockId + ":" + data);
                yield Material.STONE; // Default fallback
            }
        };
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
