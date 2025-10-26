package dev.minefaze.feudal.gui.guis;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.gui.BaseGUI;
import dev.minefaze.feudal.gui.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class KingdomBrowserGUI extends BaseGUI {
    
    public KingdomBrowserGUI(Feudal plugin, Player player) {
        super(plugin, player, "§6§lBrowse Kingdoms", 54);
    }
    
    @Override
    public void initializeItems() {
        ItemStack filler = ItemBuilder.createFiller(Material.BROWN_STAINED_GLASS_PANE);
        fillEmpty(filler);
        
        setItem(22, new ItemBuilder(Material.SPYGLASS)
            .name("§e§lKingdom Browser")
            .lore("§7Coming soon!")
            .build());
        
        setItem(49, ItemBuilder.createBackButton(),
            (p, click, item) -> plugin.getGUIManager().openKingdomMenu(p));
    }
    
    @Override
    public void refresh() {
        inventory.clear();
        initializeItems();
    }
}
