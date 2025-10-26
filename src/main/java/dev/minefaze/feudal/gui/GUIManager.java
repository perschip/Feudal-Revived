package dev.minefaze.feudal.gui;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.gui.guis.*;
import dev.minefaze.feudal.models.FeudalPlayer;
import dev.minefaze.feudal.models.Kingdom;
import org.bukkit.entity.Player;

public class GUIManager {
    
    private final Feudal plugin;
    
    public GUIManager(Feudal plugin) {
        this.plugin = plugin;
    }
    
    // Main menu GUIs
    public void openMainMenu(Player player) {
        new MainMenuGUI(plugin, player).open();
    }
    
    // Profession GUIs
    public void openProfessionsMenu(Player player) {
        new ProfessionsGUI(plugin, player).open();
    }
    
    public void openProfessionDetails(Player player, String profession) {
        new ProfessionDetailsGUI(plugin, player, profession).open();
    }
    
    // Attribute GUIs
    public void openAttributesMenu(Player player) {
        new AttributesGUI(plugin, player).open();
    }
    
    public void openAttributeDetails(Player player, String attribute) {
        new AttributeDetailsGUI(plugin, player, attribute).open();
    }
    
    // Kingdom GUIs
    public void openKingdomMenu(Player player) {
        new KingdomGUI(plugin, player).open();
    }
    
    public void openKingdomManagement(Player player) {
        new KingdomManagementGUI(plugin, player).open();
    }
    
    public void openKingdomBrowser(Player player) {
        new KingdomBrowserGUI(plugin, player).open();
    }
    
    // Challenge GUIs
    public void openChallengeMenu(Player player) {
        new ChallengeGUI(plugin, player).open();
    }
    
    public void openActiveChallenges(Player player) {
        new ActiveChallengesGUI(plugin, player).open();
    }
    
    // Market GUIs
    public void openMarketMenu(Player player) {
        new MarketGUI(plugin, player).open();
    }
    
    public void openMarketBrowser(Player player, String category) {
        new MarketBrowserGUI(plugin, player, category).open();
    }
    
    // Territory Map GUIs
    public void openTerritoryMap(Player player) {
        new TerritoryMapGUI(plugin, player).open();
    }
    
    public void openTerritoryMap(Player player, int centerX, int centerZ, int radius) {
        new TerritoryMapGUI(plugin, player, centerX, centerZ, radius).open();
    }
    
    // Utility methods
    public void closeAllGUIs(Player player) {
        if (BaseGUI.hasGUIOpen(player)) {
            BaseGUI gui = BaseGUI.getOpenGUI(player);
            if (gui != null) {
                gui.close();
            }
        }
    }
    
    public boolean hasGUIOpen(Player player) {
        return BaseGUI.hasGUIOpen(player);
    }
    
    /**
     * Open town hall selection GUI for kingdom creation
     */
    public void openTownHallSelectionGUI(Player player, String kingdomName) {
        TownHallSelectionGUI gui = new TownHallSelectionGUI(plugin, player, kingdomName);
        gui.open();
    }
    
    /**
     * Open town hall management GUI
     */
    public void openTownHallGUI(Player player) {
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (feudalPlayer == null || !feudalPlayer.hasKingdom()) {
            plugin.getMessageManager().sendMessage(player, "kingdom.must-be-in-kingdom");
            return;
        }
        
        Kingdom kingdom = feudalPlayer.getKingdom();
        if (kingdom.getTownHall() == null) {
            plugin.getMessageManager().sendMessage(player, "townhall.not-found");
            return;
        }
        
        TownHallGUI gui = new TownHallGUI(plugin, player, kingdom);
        gui.open();
    }
}
