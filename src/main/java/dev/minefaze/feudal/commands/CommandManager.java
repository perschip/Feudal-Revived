package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.commands.HelpCommand;
import dev.minefaze.feudal.commands.MenuCommand;
import dev.minefaze.feudal.commands.SubCommand;
import dev.minefaze.feudal.commands.admin.ReloadCommand;
import dev.minefaze.feudal.commands.diplomacy.*;
import dev.minefaze.feudal.commands.kingdom.*;
import dev.minefaze.feudal.commands.nation.NationCommand;
import dev.minefaze.feudal.commands.player.*;
import dev.minefaze.feudal.commands.structure.*;
import dev.minefaze.feudal.commands.territory.*;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandManager {
    
    private final Feudal plugin;
    private final Map<String, SubCommand> subCommands;
    
    public CommandManager(Feudal plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        registerCommands();
    }
    
    private void registerCommands() {
        // Register all subcommands here
        registerCommand(new HelpCommand(plugin, this));
        registerCommand(new MenuCommand(plugin));
        registerCommand(new StatsCommand(plugin));
        
        // Kingdom management commands (shortcuts)
        registerCommand(new CreateCommand(plugin));
        registerCommand(new JoinCommand(plugin));
        registerCommand(new LeaveCommand(plugin));
        registerCommand(new DisbandCommand(plugin));
        registerCommand(new ListCommand(plugin));
        registerCommand(new InfoCommand(plugin));
        registerCommand(new ClaimCommand(plugin));
        registerCommand(new KingdomCommand(plugin)); // Keep for backward compatibility
        
        // Alliance and Nation commands
        registerCommand(new AllyCommand(plugin));
        registerCommand(new EnemyCommand(plugin));
        registerCommand(new WarCommand(plugin));
        registerCommand(new NationCommand(plugin));
        
        // Challenge commands
        registerCommand(new ChallengeCommand(plugin));
        registerCommand(new AcceptCommand(plugin));
        registerCommand(new DeclineCommand(plugin));
        registerCommand(new CancelCommand(plugin));
        
        // Other commands
        registerCommand(new TerritoryCommand(plugin)); // Keep for info command
        registerCommand(new MapCommand(plugin));
        registerCommand(new TownHallCommand(plugin));
        registerCommand(new ReloadCommand(plugin));
        registerCommand(new ProfessionCommand(plugin));
        registerCommand(new AttributeCommand(plugin));
        registerCommand(new SchematicCommand(plugin));
        registerCommand(new NexusCommand(plugin));
        
        // Admin commands
        registerCommand(new AdminCommand(plugin));
    }
    
    public void registerCommand(SubCommand command) {
        subCommands.put(command.getName().toLowerCase(), command);
    }
    
    public boolean executeCommand(Player player, String[] args) {
        if (args.length == 0) {
            // Open main menu when no arguments provided
            SubCommand menuCommand = subCommands.get("menu");
            if (menuCommand != null) {
                return menuCommand.execute(player, new String[0]);
            }
            return false;
        }
        
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);
        
        if (subCommand == null) {
            player.sendMessage("§cUnknown command. Use §e/feudal help §cfor available commands.");
            return true;
        }
        
        if (!subCommand.hasPermission(player)) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        
        // Remove the subcommand name from args
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return subCommand.execute(player, subArgs);
    }
    
    public List<String> getTabCompletions(Player player, String[] args) {
        if (args.length <= 1) {
            // Complete subcommand names
            String partial = args.length == 1 ? args[0].toLowerCase() : "";
            return subCommands.keySet().stream()
                .filter(name -> name.startsWith(partial))
                .filter(name -> subCommands.get(name).hasPermission(player))
                .sorted()
                .toList();
        }
        
        // Complete subcommand arguments
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);
        
        if (subCommand != null && subCommand.hasPermission(player)) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return subCommand.getTabCompletions(player, subArgs);
        }
        
        return new ArrayList<>();
    }
    
    public Collection<SubCommand> getSubCommands() {
        return subCommands.values();
    }
    
    public SubCommand getSubCommand(String name) {
        return subCommands.get(name.toLowerCase());
    }
}
