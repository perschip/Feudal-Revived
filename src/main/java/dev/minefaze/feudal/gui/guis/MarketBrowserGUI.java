package dev.minefaze.feudal.gui.guis;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.gui.BaseGUI;
import dev.minefaze.feudal.gui.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MarketBrowserGUI extends BaseGUI {
    
    private final String category;
    
    public MarketBrowserGUI(Feudal plugin, Player player, String category) {
        super(plugin, player, "§a§lMarket - " + category, 54);
        this.category = category;
    }
    
    @Override
    public void initializeItems() {
        ItemStack filler = ItemBuilder.createFiller(Material.LIME_STAINED_GLASS_PANE);
        fillEmpty(filler);
        
        setItem(22, new ItemBuilder(Material.CHEST)
            .name("§e§lMarket Browser")
            .lore("§7Category: " + category, "§7Coming soon!")
            .build());
        
        setItem(49, ItemBuilder.createBackButton(),
            (p, click, item) -> plugin.getGUIManager().openMarketMenu(p));
    }
    
    @Override
    public void refresh() {
        inventory.clear();
        initializeItems();
    }
}
