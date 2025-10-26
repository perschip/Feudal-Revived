package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NationCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public NationCommand(Feudal plugin) {
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
            player.sendMessage("§cYou must be in a kingdom to manage nations!");
            return true;
        }
        
        switch (action) {
            case "create" -> handleCreate(player, feudalPlayer, args);
            case "join" -> handleJoin(player, feudalPlayer, args);
            case "leave" -> handleLeave(player, feudalPlayer);
            case "info" -> handleInfo(player, feudalPlayer);
            case "invite" -> handleInvite(player, feudalPlayer, args);
            default -> player.sendMessage("§cInvalid action. Use: create, join, leave, info, or invite");
        }
        
        return true;
    }
    
    private void handleCreate(Player player, FeudalPlayer feudalPlayer, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /f nation create <name>");
            return;
        }
        
        if (!feudalPlayer.isKingdomLeader()) {
            player.sendMessage("§cOnly kingdom leaders can create nations!");
            return;
        }
        
        Nation existingNation = plugin.getAllianceManager().getKingdomNation(feudalPlayer.getKingdom().getKingdomId());
        if (existingNation != null) {
            player.sendMessage("§cYour kingdom is already in the nation: " + existingNation.getName());
            return;
        }
        
        String nationName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Nation nation = plugin.getAllianceManager().createNation(nationName, feudalPlayer.getKingdom().getKingdomId());
        
        if (nation != null) {
            player.sendMessage("§6§lNation Created! §7You have founded the nation of §e" + nationName + "§7!");
        } else {
            player.sendMessage("§cFailed to create nation. Name might already be taken or your kingdom is already in a nation.");
        }
    }
    
    private void handleJoin(Player player, FeudalPlayer feudalPlayer, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /f nation join <name>");
            return;
        }
        
        if (!feudalPlayer.isKingdomLeader()) {
            player.sendMessage("§cOnly kingdom leaders can join nations!");
            return;
        }
        
        String nationName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Nation nation = plugin.getAllianceManager().getAllNations().stream()
            .filter(n -> n.getName().equalsIgnoreCase(nationName))
            .findFirst()
            .orElse(null);
        
        if (nation == null) {
            player.sendMessage("§cNation '" + nationName + "' not found!");
            return;
        }
        
        if (plugin.getAllianceManager().joinNation(nation.getNationId(), feudalPlayer.getKingdom().getKingdomId())) {
            player.sendMessage("§a§lJoined Nation! §7Your kingdom has joined " + nation.getName() + "!");
        } else {
            player.sendMessage("§cFailed to join nation. You may already be in a nation or need an invitation.");
        }
    }
    
    private void handleLeave(Player player, FeudalPlayer feudalPlayer) {
        if (!feudalPlayer.isKingdomLeader()) {
            player.sendMessage("§cOnly kingdom leaders can leave nations!");
            return;
        }
        
        Nation nation = plugin.getAllianceManager().getKingdomNation(feudalPlayer.getKingdom().getKingdomId());
        if (nation == null) {
            player.sendMessage("§cYour kingdom is not in a nation!");
            return;
        }
        
        if (nation.isLeader(feudalPlayer.getKingdom().getKingdomId()) && nation.getMemberCount() > 1) {
            player.sendMessage("§cYou cannot leave the nation as leader while other kingdoms are still members!");
            return;
        }
        
        if (plugin.getAllianceManager().leaveNation(feudalPlayer.getKingdom().getKingdomId())) {
            player.sendMessage("§e§lLeft Nation! §7Your kingdom has left " + nation.getName());
        } else {
            player.sendMessage("§cFailed to leave nation!");
        }
    }
    
    private void handleInfo(Player player, FeudalPlayer feudalPlayer) {
        Nation nation = plugin.getAllianceManager().getKingdomNation(feudalPlayer.getKingdom().getKingdomId());
        if (nation == null) {
            player.sendMessage("§cYour kingdom is not in a nation!");
            return;
        }
        
        Kingdom leaderKingdom = plugin.getKingdomManager().getKingdom(nation.getLeaderKingdomId());
        
        player.sendMessage("§6§l=== " + nation.getName() + " ===");
        player.sendMessage("§7Leader Kingdom: §e" + (leaderKingdom != null ? leaderKingdom.getName() : "Unknown"));
        player.sendMessage("§7Member Kingdoms: §e" + nation.getMemberCount());
        player.sendMessage("§7Total Power: §e" + nation.getTotalPower());
        player.sendMessage("§7Treasury: §e" + nation.getTreasury() + " coins");
        player.sendMessage("§7Created: §e" + nation.getCreatedAt().toLocalDate());
        
        if (!nation.getDescription().isEmpty()) {
            player.sendMessage("§7Description: §e" + nation.getDescription());
        }
        
        player.sendMessage("§7Member Kingdoms:");
        for (java.util.UUID kingdomId : nation.getMemberKingdomIds()) {
            Kingdom kingdom = plugin.getKingdomManager().getKingdom(kingdomId);
            if (kingdom != null) {
                String prefix = nation.isLeader(kingdomId) ? "§6[Leader] " : "§7";
                player.sendMessage("§8• " + prefix + kingdom.getName());
            }
        }
    }
    
    private void handleInvite(Player player, FeudalPlayer feudalPlayer, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /f nation invite <kingdom>");
            return;
        }
        
        Nation nation = plugin.getAllianceManager().getKingdomNation(feudalPlayer.getKingdom().getKingdomId());
        if (nation == null) {
            player.sendMessage("§cYour kingdom is not in a nation!");
            return;
        }
        
        if (!nation.isLeader(feudalPlayer.getKingdom().getKingdomId())) {
            player.sendMessage("§cOnly the nation leader can invite kingdoms!");
            return;
        }
        
        String targetKingdomName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Kingdom targetKingdom = plugin.getKingdomManager().getKingdomByName(targetKingdomName);
        
        if (targetKingdom == null) {
            player.sendMessage("§cKingdom '" + targetKingdomName + "' not found!");
            return;
        }
        
        // TODO: Implement invitation system
        player.sendMessage("§a§lInvitation Sent! §7" + targetKingdom.getName() + " has been invited to join " + nation.getName());
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "join", "leave", "info", "invite"));
        } else if (args.length == 2) {
            String action = args[0].toLowerCase();
            if ("join".equals(action)) {
                String partial = args[1].toLowerCase();
                completions = plugin.getAllianceManager().getAllNations().stream()
                    .map(Nation::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
            } else if ("invite".equals(action)) {
                String partial = args[1].toLowerCase();
                FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
                if (feudalPlayer != null && feudalPlayer.hasKingdom()) {
                    completions = plugin.getKingdomManager().getAllKingdoms().stream()
                        .filter(k -> !k.getKingdomId().equals(feudalPlayer.getKingdom().getKingdomId()))
                        .filter(k -> plugin.getAllianceManager().getKingdomNation(k.getKingdomId()) == null)
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
        return "nation";
    }
    
    @Override
    public String getDescription() {
        return "Manage nations (groups of allied kingdoms)";
    }
    
    @Override
    public String getUsage() {
        return "<create|join|leave|info|invite>";
    }
}
