package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class WarCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public WarCommand(Feudal plugin) {
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
        
        if (!feudalPlayer.hasKingdom()) {
            player.sendMessage("§cYou must be in a kingdom to manage wars!");
            return true;
        }
        
        switch (action) {
            case "declare" -> handleDeclare(player, feudalPlayer, args);
            case "end" -> handleEnd(player, feudalPlayer, args);
            case "list" -> handleList(player, feudalPlayer);
            case "info" -> handleInfo(player, feudalPlayer, args);
            default -> player.sendMessage("§cInvalid action. Use: declare, end, list, or info");
        }
        
        return true;
    }
    
    private void handleDeclare(Player player, FeudalPlayer feudalPlayer, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /f war declare <kingdom>");
            return;
        }
        
        if (!feudalPlayer.isKingdomLeader()) {
            player.sendMessage("§cOnly kingdom leaders can declare war!");
            return;
        }
        
        String targetKingdomName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Kingdom targetKingdom = plugin.getKingdomManager().getKingdomByName(targetKingdomName);
        
        if (targetKingdom == null) {
            player.sendMessage("§cKingdom '" + targetKingdomName + "' not found!");
            return;
        }
        
        UUID playerKingdomId = feudalPlayer.getKingdom().getKingdomId();
        UUID targetKingdomId = targetKingdom.getKingdomId();
        
        if (playerKingdomId.equals(targetKingdomId)) {
            player.sendMessage("§cYou cannot declare war on your own kingdom!");
            return;
        }
        
        // Check if they're nation allies
        if (plugin.getAllianceManager().areNationAllies(playerKingdomId, targetKingdomId)) {
            player.sendMessage("§cYou cannot declare war on your nation allies!");
            return;
        }
        
        // Check if already at war
        if (plugin.getAllianceManager().areEnemies(playerKingdomId, targetKingdomId)) {
            player.sendMessage("§cYou are already at war with " + targetKingdom.getName() + "!");
            return;
        }
        
        // Declare war
        Alliance war = plugin.getAllianceManager().createAlliance(
            playerKingdomId, targetKingdomId, Alliance.AllianceType.ENEMY);
        
        if (war != null) {
            player.sendMessage("§c§lWar Declared! §7Your kingdom has declared war on " + targetKingdom.getName());
            
            // Notify all nation allies about the war
            Nation playerNation = plugin.getAllianceManager().getKingdomNation(playerKingdomId);
            if (playerNation != null) {
                player.sendMessage("§6§lNation Support! §7Your nation allies will support you in this war.");
            }
        } else {
            player.sendMessage("§cFailed to declare war!");
        }
    }
    
    private void handleEnd(Player player, FeudalPlayer feudalPlayer, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /f war end <kingdom>");
            return;
        }
        
        if (!feudalPlayer.isKingdomLeader()) {
            player.sendMessage("§cOnly kingdom leaders can end wars!");
            return;
        }
        
        String targetKingdomName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Kingdom targetKingdom = plugin.getKingdomManager().getKingdomByName(targetKingdomName);
        
        if (targetKingdom == null) {
            player.sendMessage("§cKingdom '" + targetKingdomName + "' not found!");
            return;
        }
        
        UUID playerKingdomId = feudalPlayer.getKingdom().getKingdomId();
        UUID targetKingdomId = targetKingdom.getKingdomId();
        
        if (!plugin.getAllianceManager().areEnemies(playerKingdomId, targetKingdomId)) {
            player.sendMessage("§cYou are not at war with " + targetKingdom.getName() + "!");
            return;
        }
        
        if (plugin.getAllianceManager().removeAlliance(playerKingdomId, targetKingdomId)) {
            player.sendMessage("§e§lWar Ended! §7The war with " + targetKingdom.getName() + " has ended.");
        } else {
            player.sendMessage("§cFailed to end war!");
        }
    }
    
    private void handleList(Player player, FeudalPlayer feudalPlayer) {
        UUID kingdomId = feudalPlayer.getKingdom().getKingdomId();
        List<Alliance> wars = plugin.getAllianceManager().getKingdomAlliances(kingdomId).stream()
            .filter(Alliance::isEnemy)
            .collect(Collectors.toList());
        
        if (wars.isEmpty()) {
            player.sendMessage("§7Your kingdom is not at war with anyone.");
            return;
        }
        
        player.sendMessage("§c§l=== Active Wars ===");
        for (Alliance war : wars) {
            UUID enemyKingdomId = war.getOtherKingdom(kingdomId);
            Kingdom enemyKingdom = plugin.getKingdomManager().getKingdom(enemyKingdomId);
            
            if (enemyKingdom != null) {
                player.sendMessage("§8• §c" + enemyKingdom.getName() + " §7(War since " + 
                    war.getCreatedAt().toLocalDate() + ")");
            }
        }
        
        // Show nation support
        Nation nation = plugin.getAllianceManager().getKingdomNation(kingdomId);
        if (nation != null && nation.getMemberCount() > 1) {
            player.sendMessage("");
            player.sendMessage("§6§lNation Support Available:");
            for (java.util.UUID allyKingdomId : nation.getMemberKingdomIds()) {
                if (!allyKingdomId.equals(kingdomId)) {
                    Kingdom allyKingdom = plugin.getKingdomManager().getKingdom(allyKingdomId);
                    if (allyKingdom != null) {
                        player.sendMessage("§8• §6" + allyKingdom.getName() + " §7(Nation Ally)");
                    }
                }
            }
        }
    }
    
    private void handleInfo(Player player, FeudalPlayer feudalPlayer, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /f war info <kingdom>");
            return;
        }
        
        String targetKingdomName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Kingdom targetKingdom = plugin.getKingdomManager().getKingdomByName(targetKingdomName);
        
        if (targetKingdom == null) {
            player.sendMessage("§cKingdom '" + targetKingdomName + "' not found!");
            return;
        }
        
        UUID playerKingdomId = feudalPlayer.getKingdom().getKingdomId();
        UUID targetKingdomId = targetKingdom.getKingdomId();
        
        Alliance relationship = plugin.getAllianceManager().getAlliance(playerKingdomId, targetKingdomId);
        
        player.sendMessage("§6§l=== War Info: " + targetKingdom.getName() + " ===");
        player.sendMessage("§7Relationship: " + (relationship != null ? 
            relationship.getType().getColoredName() : "§7Neutral"));
        player.sendMessage("§7Their Power: §e" + targetKingdom.getPowerLevel());
        player.sendMessage("§7Their Territories: §e" + targetKingdom.getTerritoryCount());
        player.sendMessage("§7Their Members: §e" + targetKingdom.getMemberCount());
        
        // Show their nation allies
        Nation enemyNation = plugin.getAllianceManager().getKingdomNation(targetKingdomId);
        if (enemyNation != null) {
            player.sendMessage("§7Nation: §c" + enemyNation.getName() + " §7(" + 
                enemyNation.getMemberCount() + " kingdoms)");
        }
        
        if (relationship != null && relationship.isEnemy()) {
            player.sendMessage("§7War Duration: §e" + 
                java.time.Duration.between(relationship.getCreatedAt(), java.time.LocalDateTime.now()).toDays() + " days");
        }
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("declare", "end", "list", "info"));
        } else if (args.length == 2) {
            String action = args[0].toLowerCase();
            if ("declare".equals(action) || "end".equals(action) || "info".equals(action)) {
                String partial = args[1].toLowerCase();
                FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
                if (feudalPlayer != null && feudalPlayer.hasKingdom()) {
                    completions = plugin.getKingdomManager().getAllKingdoms().stream()
                        .filter(k -> !k.getKingdomId().equals(feudalPlayer.getKingdom().getKingdomId()))
                        .map(Kingdom::getName)
                        .filter(name -> name.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
                }
            }
        }
        
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
    
    @Override
    public String getName() {
        return "war";
    }
    
    @Override
    public String getDescription() {
        return "Manage wars between kingdoms";
    }
    
    @Override
    public String getUsage() {
        return "<declare|end|list|info>";
    }
}
