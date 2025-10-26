package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public ReloadCommand(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        if (!player.hasPermission("feudal.admin.reload")) {
            plugin.getMessageManager().sendMessage(player, "general.no-permission");
            return true;
        }
        
        // Reload config
        plugin.reloadConfig();
        
        // Reload language files
        plugin.getMessageManager().reload();
        
        plugin.getMessageManager().sendMessage(player, "success.action-completed");
        player.sendMessage("§aConfig and language files reloaded!");
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        return new ArrayList<>();
    }
    
    @Override
    public String getName() {
        return "reload";
    }
    
    @Override
    public String getDescription() {
        return "Reload plugin configuration and language files";
    }
    
    @Override
    public String getUsage() {
        return "";
    }
    
    @Override
    public boolean hasPermission(Player player) {
        return player.hasPermission("feudal.admin.reload");
    }
}
