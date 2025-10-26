package dev.minefaze.feudal.gui.guis;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.gui.BaseGUI;
import dev.minefaze.feudal.gui.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ChallengeGUI extends BaseGUI {
    
    public ChallengeGUI(Feudal plugin, Player player) {
        super(plugin, player, "§c§lChallenges", 45);
    }
    
    @Override
    public void initializeItems() {
        // Fill background
        ItemStack filler = ItemBuilder.createFiller(Material.RED_STAINED_GLASS_PANE);
        fillEmpty(filler);
        
        // TODO: Implement challenge management GUI
        setItem(22, new ItemBuilder(Material.IRON_SWORD)
            .name("§e§lChallenge System")
            .lore("§7Challenge system coming soon!")
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
