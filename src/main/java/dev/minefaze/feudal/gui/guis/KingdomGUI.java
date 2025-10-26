package dev.minefaze.feudal.gui.guis;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.gui.BaseGUI;
import dev.minefaze.feudal.gui.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class KingdomGUI extends BaseGUI {
    
    public KingdomGUI(Feudal plugin, Player player) {
        super(plugin, player, "§6§lKingdoms", 45);
    }
    
    @Override
    public void initializeItems() {
        // Fill background
        ItemStack filler = ItemBuilder.createFiller(Material.YELLOW_STAINED_GLASS_PANE);
        fillEmpty(filler);
        
        // TODO: Implement kingdom management GUI
        setItem(22, new ItemBuilder(Material.GOLDEN_SWORD)
            .name("§e§lKingdom Management")
            .lore("§7Kingdom system coming soon!")
            .build());
        
        // Back button
        setItem(40, ItemBuilder.createBackButton(),
            (p, click, item) -> plugin.getGUIManager().openMainMenu(p));
    }
    
    @Override
    public void refresh() {
        inventory.clear();
        initializeItems();
    }
}
