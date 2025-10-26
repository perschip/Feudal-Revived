package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AcceptCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public AcceptCommand(Feudal plugin) {
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
            if (plugin.getChallengeManager().acceptChallenge(challengeId, player.getUniqueId())) {
                plugin.getMessageManager().sendMessage(player, "challenge.accepted");
            } else {
                plugin.getMessageManager().sendMessage(player, "challenge.failed-accept");
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
        return "accept";
    }
    
    @Override
    public String getDescription() {
        return "Accept a challenge";
    }
    
    @Override
    public String getUsage() {
        return "<challengeId>";
    }
}
