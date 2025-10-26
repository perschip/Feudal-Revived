package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class StatsCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public StatsCommand(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : player;
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }
        
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getPlayer(target.getUniqueId());
        if (feudalPlayer == null) {
            player.sendMessage("§cNo data found for that player!");
            return true;
        }
        
        player.sendMessage("§6§l=== " + target.getName() + "'s Stats ===");
        player.sendMessage("§7Total Experience: §e" + feudalPlayer.getTotalExperience());
        player.sendMessage("§7Combat Power: §c" + feudalPlayer.getCombatPower());
        
        if (feudalPlayer.hasKingdom()) {
            player.sendMessage("§7Kingdom: §a" + feudalPlayer.getKingdom().getName());
        } else {
            player.sendMessage("§7Kingdom: §cNone");
        }
        
        player.sendMessage("§6§lProfessions:");
        for (Profession profession : Profession.values()) {
            int level = feudalPlayer.getProfessionLevel(profession);
            player.sendMessage("§7" + profession.getDisplayName() + ": §e" + level);
        }
        
        player.sendMessage("§6§lAttributes:");
        for (Attribute attribute : Attribute.values()) {
            int value = feudalPlayer.getAttribute(attribute);
            player.sendMessage("§7" + attribute.getDisplayName() + ": §e" + value);
        }
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().toLowerCase().startsWith(partial)) {
                    completions.add(onlinePlayer.getName());
                }
            }
        }
        
        return completions;
    }
    
    @Override
    public String getName() {
        return "stats";
    }
    
    @Override
    public String getDescription() {
        return "View player statistics";
    }
    
    @Override
    public String getUsage() {
        return "[player]";
    }
}
