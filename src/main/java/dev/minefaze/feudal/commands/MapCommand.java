package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public MapCommand(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length == 0) {
            // Default: open GUI map
            plugin.getGUIManager().openTerritoryMap(player);
            return true;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "gui" -> {
                plugin.getGUIManager().openTerritoryMap(player);
            }
            case "show" -> {
                int radius = args.length > 1 ? parseRadius(args[1], 5) : 5;
                plugin.getTerritoryVisualizationManager().showTerritoryMap(player, radius);
            }
            case "particles" -> {
                int radius = args.length > 1 ? parseRadius(args[1], 3) : 3;
                plugin.getTerritoryVisualizationManager().toggleVisualization(player, radius);
            }
            case "stop" -> {
                plugin.getTerritoryVisualizationManager().stopVisualization(player);
            }
            default -> {
                player.sendMessage("§cUsage: " + getUsage());
                player.sendMessage("§7Available actions:");
                player.sendMessage("§8• §e/f map §7- Open interactive territory map GUI");
                player.sendMessage("§8• §e/f map gui §7- Open interactive territory map GUI");
                player.sendMessage("§8• §e/f map show [radius] §7- Show territory map in chat");
                player.sendMessage("§8• §e/f map particles [radius] §7- Toggle particle borders");
                player.sendMessage("§8• §e/f map stop §7- Stop particle visualization");
            }
        }
        
        return true;
    }
    
    private int parseRadius(String radiusStr, int defaultRadius) {
        try {
            int radius = Integer.parseInt(radiusStr);
            return Math.max(1, Math.min(10, radius)); // Limit between 1-10
        } catch (NumberFormatException e) {
            return defaultRadius;
        }
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("gui", "show", "particles", "stop"));
        } else if (args.length == 2) {
            String action = args[0].toLowerCase();
            if ("show".equals(action) || "particles".equals(action)) {
                completions.addAll(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"));
            }
        }
        
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .toList();
    }
    
    @Override
    public String getName() {
        return "map";
    }
    
    @Override
    public String getDescription() {
        return "Interactive territory map with GUI and particle borders";
    }
    
    @Override
    public String getUsage() {
        return "[gui|show|particles|stop] [radius]";
    }
}
