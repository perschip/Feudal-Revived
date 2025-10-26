package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChallengeCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public ChallengeCommand(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: " + getUsage());
            player.sendMessage("§7Types: land_conquest, honor_duel, resource_raid");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cPlayer not found or not online!");
            return true;
        }
        
        if (target.equals(player)) {
            player.sendMessage("§cYou cannot challenge yourself!");
            return true;
        }
        
        ChallengeType type = ChallengeType.fromString(args[1]);
        if (type == null) {
            player.sendMessage("§cInvalid challenge type! Available: land_conquest, honor_duel, resource_raid");
            return true;
        }
        
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        if (!feudalPlayer.canChallenge()) {
            player.sendMessage("§cYou cannot issue challenges right now!");
            return true;
        }
        
        Territory targetTerritory = null;
        if (type == ChallengeType.LAND_CONQUEST) {
            targetTerritory = plugin.getKingdomManager().getTerritoryAt(target.getLocation());
            if (targetTerritory == null) {
                player.sendMessage("§cTarget player must be in claimed territory for land conquest!");
                return true;
            }
        }
        
        Challenge challenge = plugin.getChallengeManager().createChallenge(
            player.getUniqueId(), target.getUniqueId(), type, targetTerritory);
        
        if (challenge != null) {
            player.sendMessage("§6§lChallenge Sent! §7Your " + type.getDisplayName() + " challenge has been sent to " + target.getName());
        } else {
            player.sendMessage("§cFailed to create challenge!");
        }
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player) && onlinePlayer.getName().toLowerCase().startsWith(partial)) {
                    completions.add(onlinePlayer.getName());
                }
            }
        } else if (args.length == 2) {
            completions.addAll(Arrays.asList("land_conquest", "honor_duel", "resource_raid"));
        }
        
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .toList();
    }
    
    @Override
    public String getName() {
        return "challenge";
    }
    
    @Override
    public String getDescription() {
        return "Challenge another player to combat";
    }
    
    @Override
    public String getUsage() {
        return "<player> <type>";
    }
}
