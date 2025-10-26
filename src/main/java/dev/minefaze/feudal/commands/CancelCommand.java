package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CancelCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public CancelCommand(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 1) {
            plugin.getMessageManager().sendMessage(player, "commands.usage", getUsage());
            return true;
        }
        
        try {
            UUID challengeId = UUID.fromString(args[0]);
            if (plugin.getChallengeManager().cancelChallenge(challengeId, player.getUniqueId())) {
                plugin.getMessageManager().sendMessage(player, "challenge.cancelled");
            } else {
                plugin.getMessageManager().sendMessage(player, "challenge.failed-cancel");
            }
        } catch (IllegalArgumentException e) {
            plugin.getMessageManager().sendMessage(player, "general.invalid-challenge-id");
        }
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        return new ArrayList<>();
    }
    
    @Override
    public String getName() {
        return "cancel";
    }
    
    @Override
    public String getDescription() {
        return "Cancel your challenge";
    }
    
    @Override
    public String getUsage() {
        return "<challengeId>";
    }
}
