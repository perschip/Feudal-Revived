package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KingdomCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public KingdomCommand(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cUsage: " + getUsage());
            return true;
        }
        
        String action = args[0].toLowerCase();
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        
        switch (action) {
            case "create" -> handleCreate(player, feudalPlayer, args);
            case "join" -> handleJoin(player, feudalPlayer, args);
            case "leave" -> handleLeave(player, feudalPlayer);
            case "info" -> handleInfo(player, feudalPlayer);
            default -> player.sendMessage("§cInvalid action. Use: create, join, leave, or info");
        }
        
        return true;
    }
    
    private void handleCreate(Player player, FeudalPlayer feudalPlayer, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /feudal kingdom create <name>");
            return;
        }
        
        if (feudalPlayer.hasKingdom()) {
            player.sendMessage("§cYou are already in a kingdom!");
            return;
        }
        
        String kingdomName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Kingdom kingdom = plugin.getKingdomManager().createKingdom(kingdomName, player.getUniqueId(), player.getLocation());
        
        if (kingdom != null) {
            player.sendMessage("§a§lKingdom Created! §7You have founded the kingdom of §e" + kingdomName + "§7!");
        } else {
            player.sendMessage("§cFailed to create kingdom. Name might already be taken.");
        }
    }
    
    private void handleJoin(Player player, FeudalPlayer feudalPlayer, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /feudal kingdom join <name>");
            return;
        }
        
        if (feudalPlayer.hasKingdom()) {
            player.sendMessage("§cYou are already in a kingdom!");
            return;
        }
        
        String kingdomName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Kingdom kingdom = plugin.getKingdomManager().getKingdomByName(kingdomName);
        
        if (kingdom == null) {
            player.sendMessage("§cKingdom not found!");
            return;
        }
        
        if (plugin.getKingdomManager().joinKingdom(player.getUniqueId(), kingdom.getKingdomId())) {
            player.sendMessage("§a§lWelcome! §7You have joined the kingdom of §e" + kingdomName + "§7!");
        } else {
            player.sendMessage("§cFailed to join kingdom.");
        }
    }
    
    private void handleLeave(Player player, FeudalPlayer feudalPlayer) {
        if (!feudalPlayer.hasKingdom()) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        String kingdomName = feudalPlayer.getKingdom().getName();
        if (plugin.getKingdomManager().leaveKingdom(player.getUniqueId())) {
            player.sendMessage("§e§lFarewell! §7You have left the kingdom of §e" + kingdomName + "§7.");
        } else {
            player.sendMessage("§cFailed to leave kingdom.");
        }
    }
    
    private void handleInfo(Player player, FeudalPlayer feudalPlayer) {
        Kingdom kingdom = feudalPlayer.getKingdom();
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        player.sendMessage("§6§l=== " + kingdom.getName() + " ===");
        player.sendMessage("§7Leader: §e" + Bukkit.getOfflinePlayer(kingdom.getLeader()).getName());
        player.sendMessage("§7Members: §e" + kingdom.getMemberCount());
        player.sendMessage("§7Territories: §e" + kingdom.getTerritoryCount());
        player.sendMessage("§7Treasury: §e" + kingdom.getTreasury() + " coins");
        player.sendMessage("§7Power Level: §e" + kingdom.getPowerLevel());
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "join", "leave", "info"));
        }
        
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .toList();
    }
    
    @Override
    public String getName() {
        return "kingdom";
    }
    
    @Override
    public String getDescription() {
        return "Kingdom management";
    }
    
    @Override
    public String getUsage() {
        return "<create|join|leave|info>";
    }
}
