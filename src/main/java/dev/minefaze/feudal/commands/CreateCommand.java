package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.FeudalPlayer;
import dev.minefaze.feudal.models.Kingdom;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreateCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public CreateCommand(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 1) {
            plugin.getMessageManager().sendMessage(player, "commands.usage", getUsage());
            return true;
        }
        
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        
        if (feudalPlayer.hasKingdom()) {
            plugin.getMessageManager().sendMessage(player, "townhall.already-in-kingdom");
            return true;
        }
        
        String kingdomName = String.join(" ", args);
        
        // Check if kingdom name is already taken
        if (plugin.getKingdomManager().getKingdomByName(kingdomName) != null) {
            plugin.getMessageManager().sendMessage(player, "townhall.name-taken", kingdomName);
            return true;
        }
        
        // Validate kingdom name
        if (kingdomName.length() < 3 || kingdomName.length() > 20) {
            plugin.getMessageManager().sendMessage(player, "townhall.invalid-name-length");
            return true;
        }
        
        // Open town hall selection GUI
        plugin.getGUIManager().openTownHallSelectionGUI(player, kingdomName);
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        return new ArrayList<>();
    }
    
    @Override
    public String getName() {
        return "create";
    }
    
    @Override
    public String getDescription() {
        return "Create a new kingdom";
    }
    
    @Override
    public String getUsage() {
        return "<kingdom name>";
    }
}
