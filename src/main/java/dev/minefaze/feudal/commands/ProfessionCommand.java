package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ProfessionCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public ProfessionCommand(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§6§l=== Professions ===");
            for (Profession profession : Profession.values()) {
                player.sendMessage("§e" + profession.getDisplayName() + " §7- " + profession.getDescription());
            }
            return true;
        }
        
        Profession profession = Profession.fromString(args[0]);
        if (profession == null) {
            player.sendMessage("§cInvalid profession!");
            return true;
        }
        
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        int level = feudalPlayer.getProfessionLevel(profession);
        
        player.sendMessage("§6§l=== " + profession.getDisplayName() + " ===");
        player.sendMessage("§7" + profession.getDescription());
        player.sendMessage("§7Your Level: §e" + level);
        player.sendMessage("§7Experience to Next: §e" + 
            (plugin.getPlayerDataManager().getExperienceForLevel(level + 1) - feudalPlayer.getTotalExperience()));
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (Profession profession : Profession.values()) {
                String name = profession.name().toLowerCase();
                if (name.startsWith(partial)) {
                    completions.add(name);
                }
            }
        }
        
        return completions;
    }
    
    @Override
    public String getName() {
        return "profession";
    }
    
    @Override
    public String getDescription() {
        return "View profession information";
    }
    
    @Override
    public String getUsage() {
        return "[profession]";
    }
}
