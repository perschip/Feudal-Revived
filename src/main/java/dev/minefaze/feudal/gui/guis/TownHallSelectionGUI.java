package dev.minefaze.feudal.gui.guis;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.gui.BaseGUI;
import dev.minefaze.feudal.gui.ItemBuilder;
import dev.minefaze.feudal.models.Kingdom;
import dev.minefaze.feudal.models.TownHall;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

/**
 * GUI for selecting town hall type during kingdom creation
 */
public class TownHallSelectionGUI extends BaseGUI {
    
    private final String kingdomName;
    
    public TownHallSelectionGUI(Feudal plugin, Player player, String kingdomName) {
        super(plugin, player, "§6§lSelect Town Hall Style", 27);
        this.kingdomName = kingdomName;
    }
    
    @Override
    public void initializeItems() {
        // Medieval Castle
        ItemStack medieval = new ItemBuilder(Material.COBBLESTONE)
            .name("§6§lMedieval Castle")
            .lore(Arrays.asList(
                "§7A traditional stone castle with",
                "§7towers and defensive walls.",
                "",
                "§e§lFeatures:",
                "§7• Strong defensive capabilities",
                "§7• Classic medieval architecture", 
                "§7• Stone and cobblestone materials",
                "§7• Great for defensive kingdoms",
                "",
                "§a§lClick to select!"
            ))
            .build();
        
        // Fantasy Tower
        ItemStack fantasy = new ItemBuilder(Material.PURPUR_BLOCK)
            .name("§d§lMystical Tower")
            .lore(Arrays.asList(
                "§7A magical tower with enchanted",
                "§7elements and mystical properties.",
                "",
                "§e§lFeatures:",
                "§7• Enhanced magical defenses",
                "§7• Mystical architecture",
                "§7• Wood and purpur materials",
                "§7• Great for magic-focused kingdoms",
                "",
                "§a§lClick to select!"
            ))
            .build();
        
        // Modern Fortress
        ItemStack modern = new ItemBuilder(Material.IRON_BLOCK)
            .name("§7§lFortress Base")
            .lore(Arrays.asList(
                "§7A modern military-style compound",
                "§7with advanced defensive systems.",
                "",
                "§e§lFeatures:",
                "§7• Advanced defensive systems",
                "§7• Modern architecture",
                "§7• Concrete and metal materials",
                "§7• Great for military kingdoms",
                "",
                "§a§lClick to select!"
            ))
            .build();
        
        // Info item
        ItemStack info = new ItemBuilder(Material.BOOK)
            .name("§e§lTown Hall Information")
            .lore(Arrays.asList(
                "§7Choose your town hall style carefully!",
                "§7This will determine your kingdom's",
                "§7appearance and defensive capabilities.",
                "",
                "§6§lKingdom: §e" + kingdomName,
                "",
                "§7Your town hall will be placed in the",
                "§7center of a 3x3 chunk area that will",
                "§7be automatically claimed for you.",
                "",
                "§c§lNote: §7This choice is permanent!"
            ))
            .build();
        
        // Cancel item
        ItemStack cancel = new ItemBuilder(Material.BARRIER)
            .name("§c§lCancel")
            .lore(Arrays.asList(
                "§7Cancel kingdom creation",
                "§7and return to the game."
            ))
            .build();
        
        // Set items in inventory with click handlers
        setItem(10, medieval, (player, clickType, item) -> createKingdom(player, TownHall.TownHallType.MEDIEVAL));
        setItem(13, fantasy, (player, clickType, item) -> createKingdom(player, TownHall.TownHallType.FANTASY));
        setItem(16, modern, (player, clickType, item) -> createKingdom(player, TownHall.TownHallType.MODERN));
        setItem(4, info);
        setItem(22, cancel, (player, clickType, item) -> {
            player.closeInventory();
            plugin.getMessageManager().sendMessage(player, "townhall.creation-cancelled");
        });
    }
    
    @Override
    public void refresh() {
        // This GUI doesn't need refreshing as it's static content
    }
    
    /**
     * Create the kingdom with selected town hall type
     */
    private void createKingdom(Player player, TownHall.TownHallType type) {
        player.closeInventory();
        
        // Double-check kingdom name availability before creation
        plugin.getLogger().info("GUI: Double-checking kingdom name '" + kingdomName + "' availability");
        Kingdom existingKingdom = plugin.getKingdomManager().getKingdomByName(kingdomName);
        if (existingKingdom != null) {
            plugin.getLogger().warning("GUI: Kingdom creation blocked - name '" + kingdomName + "' already exists");
            plugin.getMessageManager().sendMessage(player, "townhall.name-taken", kingdomName);
            return;
        }
        plugin.getLogger().info("GUI: Kingdom name '" + kingdomName + "' is available, proceeding with creation");
        
        // Create kingdom with 3x3 chunk claiming
        Kingdom kingdom = plugin.getKingdomManager().createKingdomWithTownHall(
            kingdomName, 
            player.getUniqueId(), 
            player.getLocation(),
            type
        );
        
        if (kingdom != null) {
            plugin.getMessageManager().sendMessage(player, "townhall.kingdom-created", 
                kingdomName, type.getDisplayName());
            
            // Show success effects
            player.sendTitle("§6§lKingdom Founded!", 
                "§7Welcome to " + kingdomName, 10, 60, 20);
            
            // Open town hall GUI after creation
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getGUIManager().openTownHallGUI(player);
            }, 60L); // 3 seconds delay
            
        } else {
            plugin.getMessageManager().sendMessage(player, "townhall.creation-failed");
        }
    }
}
