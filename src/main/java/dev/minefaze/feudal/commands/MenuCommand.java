package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MenuCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public MenuCommand(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        plugin.getGUIManager().openMainMenu(player);
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        return new ArrayList<>();
    }
    
    @Override
    public String getName() {
        return "menu";
    }
    
    @Override
    public String getDescription() {
        return "Open the main Feudal RPG menu";
    }
    
    @Override
    public String getUsage() {
        return "";
    }
}
