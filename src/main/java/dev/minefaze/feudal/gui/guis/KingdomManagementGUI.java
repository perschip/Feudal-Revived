package dev.minefaze.feudal.gui.guis;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.gui.BaseGUI;
import dev.minefaze.feudal.gui.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class KingdomManagementGUI extends BaseGUI {
    
    public KingdomManagementGUI(Feudal plugin, Player player) {
        super(plugin, player, "§6§lKingdom Management", 36);
    }
    
    @Override
    public void initializeItems() {
        ItemStack filler = ItemBuilder.createFiller(Material.ORANGE_STAINED_GLASS_PANE);
        fillEmpty(filler);
        
        setItem(13, new ItemBuilder(Material.GOLDEN_HELMET)
            .name("§e§lKingdom Management")
            .lore("§7Coming soon!")
            .build());
        
        setItem(31, ItemBuilder.createBackButton(),
            (p, click, item) -> plugin.getGUIManager().openKingdomMenu(p));
    }
    
    @Override
    public void refresh() {
        inventory.clear();
        initializeItems();
    }
}
