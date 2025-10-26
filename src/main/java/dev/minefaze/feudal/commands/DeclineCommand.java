package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeclineCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public DeclineCommand(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cUsage: " + getUsage());
            return true;
        }
        
        try {
            UUID challengeId = UUID.fromString(args[0]);
            if (plugin.getChallengeManager().declineChallenge(challengeId, player.getUniqueId())) {
                player.sendMessage("§e§lChallenge Declined! §7You have declined the challenge.");
            } else {
                player.sendMessage("§cFailed to decline challenge!");
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid challenge ID!");
        }
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        return new ArrayList<>();
    }
    
    @Override
    public String getName() {
        return "decline";
    }
    
    @Override
    public String getDescription() {
        return "Decline a challenge";
    }
    
    @Override
    public String getUsage() {
        return "<challengeId>";
    }
}
