package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TerritoryCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public TerritoryCommand(Feudal plugin) {
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
            case "claim" -> handleClaim(player, feudalPlayer);
            case "info" -> handleInfo(player);
            default -> player.sendMessage("§cInvalid action. Use: claim or info");
        }
        
        return true;
    }
    
    private void handleClaim(Player player, FeudalPlayer feudalPlayer) {
        if (!feudalPlayer.hasKingdom()) {
            player.sendMessage("§cYou must be in a kingdom to claim territory!");
            return;
        }
        
        if (!feudalPlayer.isKingdomLeader()) {
            player.sendMessage("§cOnly kingdom leaders can claim territory!");
            return;
        }
        
        if (plugin.getKingdomManager().claimTerritory(
            feudalPlayer.getKingdom().getKingdomId(), 
            player.getLocation().getChunk(), 
            TerritoryType.OUTPOST)) {
            player.sendMessage("§a§lTerritory Claimed! §7This chunk now belongs to your kingdom!");
        } else {
            player.sendMessage("§cFailed to claim territory! It may already be claimed or your kingdom may be at its limit.");
        }
    }
    
    private void handleInfo(Player player) {
        Territory territory = plugin.getKingdomManager().getTerritoryAt(player.getLocation());
        if (territory == null) {
            player.sendMessage("§7This area is unclaimed wilderness.");
            return;
        }
        
        Kingdom kingdom = plugin.getKingdomManager().getKingdom(territory.getKingdomId());
        player.sendMessage("§6§l=== Territory Info ===");
        player.sendMessage("§7Owner: §e" + (kingdom != null ? kingdom.getName() : "Unknown"));
        player.sendMessage("§7Type: §e" + territory.getType().getDisplayName());
        player.sendMessage("§7Defense Level: §e" + territory.getDefenseLevel());
        player.sendMessage("§7Coordinates: §e" + territory.getCoordinates());
        
        if (territory.isUnderAttack()) {
            player.sendMessage("§c§lUNDER ATTACK! §7This territory is currently being challenged!");
        }
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("claim", "info"));
        }
        
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .toList();
    }
    
    @Override
    public String getName() {
        return "territory";
    }
    
    @Override
    public String getDescription() {
        return "Territory management";
    }
    
    @Override
    public String getUsage() {
        return "<claim|info>";
    }
}
