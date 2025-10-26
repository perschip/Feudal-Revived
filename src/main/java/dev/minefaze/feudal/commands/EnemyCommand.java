package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EnemyCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public EnemyCommand(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cUsage: " + getUsage());
            return true;
        }
        
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        if (!feudalPlayer.hasKingdom()) {
            player.sendMessage("§cYou must be in a kingdom to declare war!");
            return true;
        }
        
        if (!feudalPlayer.isKingdomLeader()) {
            player.sendMessage("§cOnly kingdom leaders can declare war!");
            return true;
        }
        
        String targetKingdomName = String.join(" ", args);
        Kingdom targetKingdom = plugin.getKingdomManager().getKingdomByName(targetKingdomName);
        
        if (targetKingdom == null) {
            player.sendMessage("§cKingdom '" + targetKingdomName + "' not found!");
            return true;
        }
        
        if (targetKingdom.getKingdomId().equals(feudalPlayer.getKingdom().getKingdomId())) {
            player.sendMessage("§cYou cannot declare war on your own kingdom!");
            return true;
        }
        
        // Check if they're nation allies
        if (plugin.getAllianceManager().areNationAllies(
            feudalPlayer.getKingdom().getKingdomId(), 
            targetKingdom.getKingdomId())) {
            player.sendMessage("§cYou cannot declare war on your nation allies!");
            return true;
        }
        
        Alliance alliance = plugin.getAllianceManager().createAlliance(
            feudalPlayer.getKingdom().getKingdomId(),
            targetKingdom.getKingdomId(),
            Alliance.AllianceType.ENEMY
        );
        
        if (alliance != null) {
            player.sendMessage("§c§lWar Declared! §7You have declared war on " + targetKingdom.getName());
        } else {
            player.sendMessage("§cFailed to declare war!");
        }
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
            if (feudalPlayer != null && feudalPlayer.hasKingdom()) {
                completions = plugin.getKingdomManager().getAllKingdoms().stream()
                    .filter(k -> !k.getKingdomId().equals(feudalPlayer.getKingdom().getKingdomId()))
                    .filter(k -> !plugin.getAllianceManager().areNationAllies(
                        feudalPlayer.getKingdom().getKingdomId(), k.getKingdomId()))
                    .map(Kingdom::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
    
    @Override
    public String getName() {
        return "enemy";
    }
    
    @Override
    public String getDescription() {
        return "Declare war on another kingdom";
    }
    
    @Override
    public String getUsage() {
        return "<kingdom name>";
    }
}
