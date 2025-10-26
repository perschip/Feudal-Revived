package dev.minefaze.feudal.gui.guis;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.gui.BaseGUI;
import dev.minefaze.feudal.gui.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AttributeDetailsGUI extends BaseGUI {
    
    private final String attribute;
    
    public AttributeDetailsGUI(Feudal plugin, Player player, String attribute) {
        super(plugin, player, "§d§l" + attribute.toUpperCase() + " Details", 36);
        this.attribute = attribute;
    }
    
    @Override
    public void initializeItems() {
        // Fill background
        ItemStack filler = ItemBuilder.createFiller(Material.MAGENTA_STAINED_GLASS_PANE);
        fillEmpty(filler);
        
        // TODO: Implement detailed attribute view
        setItem(13, new ItemBuilder(Material.ENCHANTED_BOOK)
            .name("§e§lAttribute Details")
            .lore("§7Detailed " + attribute + " information coming soon!")
            .build());
        
        // Back button
        setItem(31, ItemBuilder.createBackButton(),
            (p, click, item) -> plugin.getGUIManager().openAttributesMenu(p));
    }
    
    @Override
    public void refresh() {
        inventory.clear();
        initializeItems();
    }
}
