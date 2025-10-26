package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class FeudalCommand implements CommandExecutor, TabCompleter {
    
    private final Feudal plugin;
    private final CommandManager commandManager;
    
    public FeudalCommand(Feudal plugin) {
        this.plugin = plugin;
        this.commandManager = new CommandManager(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        return commandManager.executeCommand(player, args);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        
        return commandManager.getTabCompletions(player, args);
    }
}
