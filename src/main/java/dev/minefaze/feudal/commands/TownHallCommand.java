package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.FeudalPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TownHallCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public TownHallCommand(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        
        if (feudalPlayer == null || !feudalPlayer.hasKingdom()) {
            plugin.getMessageManager().sendMessage(player, "kingdom.must-be-in-kingdom");
            return true;
        }
        
        if (feudalPlayer.getKingdom().getTownHall() == null) {
            plugin.getMessageManager().sendMessage(player, "townhall.not-found");
            return true;
        }
        
        // Open town hall GUI
        plugin.getGUIManager().openTownHallGUI(player);
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        return new ArrayList<>();
    }
    
    @Override
    public String getName() {
        return "townhall";
    }
    
    @Override
    public String getDescription() {
        return "Open your kingdom's town hall";
    }
    
    @Override
    public String getUsage() {
        return "";
    }
}
