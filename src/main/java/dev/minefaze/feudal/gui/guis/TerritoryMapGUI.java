package dev.minefaze.feudal.gui.guis;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.gui.BaseGUI;
import dev.minefaze.feudal.gui.ItemBuilder;
import dev.minefaze.feudal.models.FeudalPlayer;
import dev.minefaze.feudal.models.Kingdom;
import dev.minefaze.feudal.models.Territory;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TerritoryMapGUI extends BaseGUI {
    
    private int centerX, centerZ;
    private int radius;
    private final int MAP_SIZE = 5; // 5x5 grid to fit in inventory
    private final int MAP_START_SLOT = 11; // Starting slot for map display (centered)
    
    public TerritoryMapGUI(Feudal plugin, Player player) {
        this(plugin, player, player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ(), 3);
    }
    
    public TerritoryMapGUI(Feudal plugin, Player player, int centerX, int centerZ, int radius) {
        super(plugin, player, "§6Territory Map", 54); // 6 rows
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = Math.max(1, Math.min(2, radius)); // Limit radius to 1-2 for 5x5 display
    }
    
    @Override
    public void initializeItems() {
        setupGUI();
    }
    
    @Override
    public void refresh() {
        setupGUI();
    }
    
    protected void setupGUI() {
        // Clear inventory
        inventory.clear();
        
        // Add border items
        addBorderItems();
        
        // Add navigation controls
        addNavigationControls();
        
        // Add map display
        addMapDisplay();
        
        // Add legend
        addLegend();
        
        // Add close button
        addCloseButton();
    }
    
    private void addBorderItems() {
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name("§7")
                .build();
        
        // Top and bottom borders
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }
        
        // Side borders
        for (int row = 1; row < 5; row++) {
            inventory.setItem(row * 9, border);
            inventory.setItem(row * 9 + 8, border);
        }
    }
    
    private void addNavigationControls() {
        // Zoom controls
        ItemStack zoomIn = new ItemBuilder(Material.SPYGLASS)
                .name("§a§lZoom In")
                .lore("§7Click to zoom in", "§7Current radius: §e" + radius)
                .build();
        
        ItemStack zoomOut = new ItemBuilder(Material.COMPASS)
                .name("§c§lZoom Out")
                .lore("§7Click to zoom out", "§7Current radius: §e" + radius)
                .build();
        
        // Movement controls
        ItemStack moveNorth = new ItemBuilder(Material.ARROW)
                .name("§b§lMove North")
                .lore("§7Click to move map north")
                .build();
        
        ItemStack moveSouth = new ItemBuilder(Material.ARROW)
                .name("§b§lMove South")
                .lore("§7Click to move map south")
                .build();
        
        ItemStack moveWest = new ItemBuilder(Material.ARROW)
                .name("§b§lMove West")
                .lore("§7Click to move map west")
                .build();
        
        ItemStack moveEast = new ItemBuilder(Material.ARROW)
                .name("§b§lMove East")
                .lore("§7Click to move map east")
                .build();
        
        ItemStack center = new ItemBuilder(Material.RECOVERY_COMPASS)
                .name("§e§lCenter on Player")
                .lore("§7Click to center map on your location")
                .build();
        
        // Place navigation items
        inventory.setItem(1, moveNorth);
        inventory.setItem(7, zoomIn);
        inventory.setItem(19, moveWest);
        inventory.setItem(25, moveEast);
        inventory.setItem(37, moveSouth);
        inventory.setItem(43, zoomOut);
        inventory.setItem(4, center);
        
        // Add click handlers
        clickHandlers.put(1, (p, clickType, item) -> moveMap(0, -1));
        clickHandlers.put(7, (p, clickType, item) -> zoomIn());
        clickHandlers.put(19, (p, clickType, item) -> moveMap(-1, 0));
        clickHandlers.put(25, (p, clickType, item) -> moveMap(1, 0));
        clickHandlers.put(37, (p, clickType, item) -> moveMap(0, 1));
        clickHandlers.put(43, (p, clickType, item) -> zoomOut());
        clickHandlers.put(4, (p, clickType, item) -> centerOnPlayer());
    }
    
    private void addMapDisplay() {
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        UUID playerKingdomId = feudalPlayer.hasKingdom() ? feudalPlayer.getKingdom().getKingdomId() : null;
        World world = player.getWorld();
        
        // Create 5x5 map display - fits in rows 1-4 (slots 11-35)
        for (int row = 0; row < MAP_SIZE; row++) {
            for (int col = 0; col < MAP_SIZE; col++) {
                int slot = MAP_START_SLOT + row * 9 + col;
                
                // Safety check to ensure we don't exceed inventory bounds
                if (slot >= 54) {
                    plugin.getLogger().warning("Slot " + slot + " exceeds inventory bounds for row " + row + ", col " + col);
                    continue;
                }
                
                // Calculate chunk coordinates
                int chunkX = centerX + (col - radius);
                int chunkZ = centerZ + (row - radius);
                
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                Territory territory = plugin.getKingdomManager().getTerritoryAt(chunk.getBlock(8, 64, 8).getLocation());
                
                ItemStack mapItem = createMapItem(territory, playerKingdomId, chunkX, chunkZ);
                inventory.setItem(slot, mapItem);
                
                // Add click handler for territory info
                final int finalChunkX = chunkX;
                final int finalChunkZ = chunkZ;
                clickHandlers.put(slot, (p, clickType, item) -> showTerritoryInfo(territory, finalChunkX, finalChunkZ));
            }
        }
    }
    
    private ItemStack createMapItem(Territory territory, UUID playerKingdomId, int chunkX, int chunkZ) {
        Material material;
        String name;
        List<String> lore = new ArrayList<>();
        
        lore.add("§7Chunk: §e" + chunkX + ", " + chunkZ);
        
        if (territory == null) {
            material = Material.LIGHT_GRAY_CONCRETE;
            name = "§8Unclaimed";
            lore.add("§7Status: §8Unclaimed wilderness");
        } else {
            UUID territoryKingdomId = territory.getKingdomId();
            Kingdom kingdom = plugin.getKingdomManager().getKingdom(territoryKingdomId);
            String kingdomName = kingdom != null ? kingdom.getName() : "Unknown Kingdom";
            
            lore.add("§7Kingdom: §e" + kingdomName);
            lore.add("§7Type: §b" + territory.getType().name());
            
            if (playerKingdomId != null && playerKingdomId.equals(territoryKingdomId)) {
                material = Material.GREEN_CONCRETE;
                name = "§a" + kingdomName;
                lore.add("§7Relationship: §a§lYOUR KINGDOM");
            } else {
                // Check relationships
                FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
                if (feudalPlayer.hasKingdom() && kingdom != null) {
                    String relationship = plugin.getAllianceManager().getRelationshipType(
                            feudalPlayer.getKingdom(), kingdom);
                    
                    switch (relationship.toUpperCase()) {
                        case "OWN" -> {
                            material = Material.GREEN_CONCRETE;
                            name = "§a" + kingdomName;
                            lore.add("§7Relationship: §a§lYOUR KINGDOM");
                        }
                        case "ALLY" -> {
                            material = Material.BLUE_CONCRETE;
                            name = "§b" + kingdomName;
                            lore.add("§7Relationship: §b§lALLY");
                        }
                        case "ENEMY" -> {
                            material = Material.RED_CONCRETE;
                            name = "§c" + kingdomName;
                            lore.add("§7Relationship: §c§lENEMY");
                        }
                        default -> {
                            material = Material.YELLOW_CONCRETE;
                            name = "§e" + kingdomName;
                            lore.add("§7Relationship: §e§lNEUTRAL");
                        }
                    }
                } else {
                    material = Material.YELLOW_CONCRETE;
                    name = "§e" + kingdomName;
                    lore.add("§7Relationship: §e§lNEUTRAL");
                }
            }
        }
        
        lore.add("");
        lore.add("§7Click for more info");
        
        return new ItemBuilder(material)
                .name(name)
                .lore(lore)
                .build();
    }
    
    private void addLegend() {
        // Legend items
        ItemStack ownLegend = new ItemBuilder(Material.GREEN_CONCRETE)
                .name("§a§lYour Kingdom")
                .lore("§7Territories owned by your kingdom")
                .build();
        
        ItemStack allyLegend = new ItemBuilder(Material.BLUE_CONCRETE)
                .name("§b§lAllied Kingdom")
                .lore("§7Territories owned by allied kingdoms")
                .build();
        
        ItemStack enemyLegend = new ItemBuilder(Material.RED_CONCRETE)
                .name("§c§lEnemy Kingdom")
                .lore("§7Territories owned by enemy kingdoms")
                .build();
        
        ItemStack neutralLegend = new ItemBuilder(Material.YELLOW_CONCRETE)
                .name("§e§lNeutral Kingdom")
                .lore("§7Territories owned by neutral kingdoms")
                .build();
        
        ItemStack unclaimedLegend = new ItemBuilder(Material.LIGHT_GRAY_CONCRETE)
                .name("§8§lUnclaimed")
                .lore("§7Unclaimed wilderness")
                .build();
        
        // Place legend items
        inventory.setItem(46, ownLegend);
        inventory.setItem(47, allyLegend);
        inventory.setItem(48, enemyLegend);
        inventory.setItem(50, neutralLegend);
        inventory.setItem(51, unclaimedLegend);
    }
    
    private void addCloseButton() {
        ItemStack closeButton = new ItemBuilder(Material.BARRIER)
                .name("§c§lClose Map")
                .lore("§7Click to close the territory map")
                .build();
        
        inventory.setItem(49, closeButton);
        clickHandlers.put(49, (p, clickType, item) -> p.closeInventory());
    }
    
    private void moveMap(int deltaX, int deltaZ) {
        centerX += deltaX;
        centerZ += deltaZ;
        setupGUI();
        player.sendMessage("§7Map moved to §e" + centerX + ", " + centerZ);
    }
    
    private void zoomIn() {
        if (radius > 1) {
            radius--;
            setupGUI();
            player.sendMessage("§7Zoomed in! Radius: §e" + radius);
        } else {
            player.sendMessage("§cAlready at maximum zoom!");
        }
    }
    
    private void zoomOut() {
        if (radius < 2) {
            radius++;
            setupGUI();
            player.sendMessage("§7Zoomed out! Radius: §e" + radius);
        } else {
            player.sendMessage("§cAlready at minimum zoom!");
        }
    }
    
    private void centerOnPlayer() {
        Chunk playerChunk = player.getLocation().getChunk();
        centerX = playerChunk.getX();
        centerZ = playerChunk.getZ();
        setupGUI();
        player.sendMessage("§7Map centered on your location: §e" + centerX + ", " + centerZ);
    }
    
    private void showTerritoryInfo(Territory territory, int chunkX, int chunkZ) {
        if (territory == null) {
            player.sendMessage("§8§l=== Unclaimed Territory ===");
            player.sendMessage("§7Chunk: §e" + chunkX + ", " + chunkZ);
            player.sendMessage("§7Status: §8Unclaimed wilderness");
            player.sendMessage("§7You can claim this territory!");
        } else {
            player.sendMessage("§6§l=== Territory Information ===");
            player.sendMessage("§7Chunk: §e" + chunkX + ", " + chunkZ);
            Kingdom territoryKingdom = plugin.getKingdomManager().getKingdom(territory.getKingdomId());
            String territoryKingdomName = territoryKingdom != null ? territoryKingdom.getName() : "Unknown Kingdom";
            player.sendMessage("§7Kingdom: §e" + territoryKingdomName);
            player.sendMessage("§7Type: §b" + territory.getType().name());
            player.sendMessage("§7Defense Power: §c" + territory.getDefensePower());
            
            FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
            if (feudalPlayer.hasKingdom() && territoryKingdom != null) {
                String relationship = plugin.getAllianceManager().getRelationshipType(
                        feudalPlayer.getKingdom(), territoryKingdom);
                
                String relationshipDisplay = switch (relationship.toUpperCase()) {
                    case "OWN" -> "§a§lYOUR KINGDOM";
                    case "ALLY" -> "§b§lALLY";
                    case "ENEMY" -> "§c§lENEMY";
                    default -> "§e§lNEUTRAL";
                };
                
                player.sendMessage("§7Relationship: " + relationshipDisplay);
            }
        }
    }
}
