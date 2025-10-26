package dev.minefaze.feudal.commands;

import org.bukkit.entity.Player;

import java.util.List;

public interface SubCommand {
    
    /**
     * Execute the subcommand
     * @param player The player executing the command
     * @param args The command arguments (excluding the subcommand name)
     * @return true if command was handled successfully
     */
    boolean execute(Player player, String[] args);
    
    /**
     * Get tab completions for this subcommand
     * @param player The player requesting completions
     * @param args The current arguments
     * @return List of possible completions
     */
    List<String> getTabCompletions(Player player, String[] args);
    
    /**
     * Get the name of this subcommand
     * @return The command name
     */
    String getName();
    
    /**
     * Get the description of this subcommand
     * @return The command description
     */
    String getDescription();
    
    /**
     * Get the usage string for this subcommand
     * @return The usage string
     */
    String getUsage();
    
    /**
     * Check if the player has permission to use this command
     * @param player The player to check
     * @return true if player has permission
     */
    default boolean hasPermission(Player player) {
        return true; // Default: no permission required
    }
}
