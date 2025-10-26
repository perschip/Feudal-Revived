package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AllyCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public AllyCommand(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 1) {
            plugin.getMessageManager().sendMessage(player, "commands.usage", getUsage());
            return true;
        }
        
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        if (!feudalPlayer.hasKingdom()) {
            plugin.getMessageManager().sendMessage(player, "kingdom.must-be-in-kingdom");
            return true;
        }
        
        if (!feudalPlayer.isKingdomLeader()) {
            plugin.getMessageManager().sendMessage(player, "kingdom.must-be-leader");
            return true;
        }
        
        String targetKingdomName = String.join(" ", args);
        Kingdom targetKingdom = plugin.getKingdomManager().getKingdomByName(targetKingdomName);
        
        if (targetKingdom == null) {
            plugin.getMessageManager().sendMessage(player, "kingdom.not-found", targetKingdomName);
            return true;
        }
        
        if (targetKingdom.getKingdomId().equals(feudalPlayer.getKingdom().getKingdomId())) {
            plugin.getMessageManager().sendMessage(player, "kingdom.cannot-ally-self");
            return true;
        }
        
        Alliance alliance = plugin.getAllianceManager().createAlliance(
            feudalPlayer.getKingdom().getKingdomId(),
            targetKingdom.getKingdomId(),
            Alliance.AllianceType.ALLY
        );
        
        if (alliance != null) {
            plugin.getMessageManager().sendMessage(player, "alliance.proposed", targetKingdom.getName());
            // TODO: Add alliance confirmation system
        } else {
            plugin.getMessageManager().sendMessage(player, "alliance.failed");
        }
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            // Get all kingdoms except player's own
            FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
            if (feudalPlayer != null && feudalPlayer.hasKingdom()) {
                completions = plugin.getKingdomManager().getAllKingdoms().stream()
                    .filter(k -> !k.getKingdomId().equals(feudalPlayer.getKingdom().getKingdomId()))
                    .map(Kingdom::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
    
    @Override
    public String getName() {
        return "ally";
    }
    
    @Override
    public String getDescription() {
        return "Create an alliance with another kingdom";
    }
    
    @Override
    public String getUsage() {
        return "<kingdom name>";
    }
}
