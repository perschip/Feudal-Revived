package dev.minefaze.feudal.gui.guis;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.gui.BaseGUI;
import dev.minefaze.feudal.gui.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ProfessionDetailsGUI extends BaseGUI {
    
    private final String profession;
    
    public ProfessionDetailsGUI(Feudal plugin, Player player, String profession) {
        super(plugin, player, "§b§l" + profession.toUpperCase() + " Details", 36);
        this.profession = profession;
    }
    
    @Override
    public void initializeItems() {
        // Fill background
        ItemStack filler = ItemBuilder.createFiller(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        fillEmpty(filler);
        
        // TODO: Implement detailed profession view
        setItem(13, new ItemBuilder(Material.BOOK)
            .name("§e§lProfession Details")
            .lore("§7Detailed " + profession + " information coming soon!")
            .build());
        
        // Back button
        setItem(31, ItemBuilder.createBackButton(),
            (p, click, item) -> plugin.getGUIManager().openProfessionsMenu(p));
    }
    
    @Override
    public void refresh() {
        inventory.clear();
        initializeItems();
    }
}
