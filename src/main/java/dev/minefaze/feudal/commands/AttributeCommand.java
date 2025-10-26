package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AttributeCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public AttributeCommand(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§6§l=== Attributes ===");
            for (Attribute attribute : Attribute.values()) {
                player.sendMessage("§e" + attribute.getDisplayName() + " §7- " + attribute.getDescription());
            }
            return true;
        }
        
        Attribute attribute = Attribute.fromString(args[0]);
        if (attribute == null) {
            player.sendMessage("§cInvalid attribute!");
            return true;
        }
        
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        int value = feudalPlayer.getAttribute(attribute);
        
        player.sendMessage("§6§l=== " + attribute.getDisplayName() + " ===");
        player.sendMessage("§7" + attribute.getDescription());
        player.sendMessage("§7Your Value: §e" + value);
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (Attribute attribute : Attribute.values()) {
                String name = attribute.name().toLowerCase();
                if (name.startsWith(partial)) {
                    completions.add(name);
                }
            }
        }
        
        return completions;
    }
    
    @Override
    public String getName() {
        return "attribute";
    }
    
    @Override
    public String getDescription() {
        return "View attribute information";
    }
    
    @Override
    public String getUsage() {
        return "[attribute]";
    }
}
