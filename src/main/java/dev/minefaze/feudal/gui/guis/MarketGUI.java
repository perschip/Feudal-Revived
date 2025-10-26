package dev.minefaze.feudal.gui.guis;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.gui.BaseGUI;
import dev.minefaze.feudal.gui.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MarketGUI extends BaseGUI {
    
    public MarketGUI(Feudal plugin, Player player) {
        super(plugin, player, "§a§lMarket", 45);
    }
    
    @Override
    public void initializeItems() {
        // Fill background
        ItemStack filler = ItemBuilder.createFiller(Material.GREEN_STAINED_GLASS_PANE);
        fillEmpty(filler);
        
        // TODO: Implement market system GUI
        setItem(22, new ItemBuilder(Material.EMERALD)
            .name("§e§lMarket System")
            .lore("§7Market system coming soon!")
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
