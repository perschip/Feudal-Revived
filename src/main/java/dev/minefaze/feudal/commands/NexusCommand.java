package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.FeudalPlayer;
import dev.minefaze.feudal.models.Kingdom;
import dev.minefaze.feudal.models.Nexus;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command for nexus management
 */
public class NexusCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public NexusCommand(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (feudalPlayer == null || !feudalPlayer.hasKingdom()) {
            plugin.getMessageManager().sendMessage(player, "kingdom.must-be-in-kingdom");
            return true;
        }
        
        Kingdom kingdom = feudalPlayer.getKingdom();
        
        if (args.length == 0) {
            return handleInfo(player, kingdom);
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "place":
                return handlePlace(player, kingdom);
            case "remove":
                return handleRemove(player, kingdom);
            case "info":
                return handleInfo(player, kingdom);
            case "health":
                return handleHealth(player, kingdom);
            default:
                sendHelpMessage(player);
                return true;
        }
    }
    
    private boolean handlePlace(Player player, Kingdom kingdom) {
        // Check if player is kingdom leader
        if (!kingdom.isLeader(player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "kingdom.must-be-leader");
            return true;
        }
        
        // Check if kingdom already has a nexus
        if (kingdom.getNexus() != null && kingdom.getNexus().getLocation() != null) {
            plugin.getMessageManager().sendMessage(player, "nexus.already-exists");
            return true;
        }
        
        // Place nexus at player's location
        Location location = player.getLocation().getBlock().getLocation();
        location.setY(location.getY() + 1); // Place one block above ground
        
        boolean success = plugin.getNexusManager().placeNexus(kingdom, location, player);
        
        if (success) {
            plugin.getMessageManager().sendMessage(player, "nexus.placed-successfully");
        }
        
        return true;
    }
    
    private boolean handleRemove(Player player, Kingdom kingdom) {
        // Check if player is kingdom leader
        if (!kingdom.isLeader(player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "kingdom.must-be-leader");
            return true;
        }
        
        // Check if kingdom has a nexus
        if (kingdom.getNexus() == null || kingdom.getNexus().getLocation() == null) {
            plugin.getMessageManager().sendMessage(player, "nexus.not-found");
            return true;
        }
        
        boolean success = plugin.getNexusManager().removeNexus(kingdom, player);
        
        if (success) {
            plugin.getMessageManager().sendMessage(player, "nexus.removed-successfully");
        }
        
        return true;
    }
    
    private boolean handleInfo(Player player, Kingdom kingdom) {
        Nexus nexus = kingdom.getNexus();
        
        if (nexus == null || nexus.getLocation() == null) {
            plugin.getMessageManager().sendMessage(player, "nexus.not-found");
            player.sendMessage("§7Use §e/f nexus place §7to place your kingdom's nexus");
            return true;
        }
        
        Location loc = nexus.getLocation();
        
        player.sendMessage("§6§l=== Kingdom Nexus Info ===");
        player.sendMessage("§7Location: §e" + loc.getWorld().getName() + " " + 
                          loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        player.sendMessage("§7Health: §c" + nexus.getCurrentHealth() + "§7/§a" + nexus.getMaxHealth());
        
        // Health bar visualization
        double healthPercent = (double) nexus.getCurrentHealth() / nexus.getMaxHealth();
        int barLength = 20;
        int filledBars = (int) (healthPercent * barLength);
        
        StringBuilder healthBar = new StringBuilder("§7[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                if (healthPercent > 0.6) {
                    healthBar.append("§a█");
                } else if (healthPercent > 0.3) {
                    healthBar.append("§e█");
                } else {
                    healthBar.append("§c█");
                }
            } else {
                healthBar.append("§8█");
            }
        }
        healthBar.append("§7]");
        
        player.sendMessage(healthBar.toString());
        player.sendMessage("§7Status: §e" + nexus.getStatus().getDisplayName());
        player.sendMessage("§7Shield Points: §b" + nexus.getShieldPoints());
        player.sendMessage("§7Armor: §7" + nexus.getArmor());
        player.sendMessage("§7Magic Resistance: §d" + nexus.getMagicResistance() + "%");
        
        if (nexus.isRegenerating()) {
            player.sendMessage("§a§lRegenerating §7(+" + nexus.getRegenerationRate() + " HP/min)");
        } else if (nexus.getLastDamageTime() > 0) {
            long timeSinceLastDamage = System.currentTimeMillis() - nexus.getLastDamageTime();
            long regenDelay = plugin.getConfig().getInt("nexus.regen-delay", 5) * 60 * 1000;
            
            if (timeSinceLastDamage < regenDelay) {
                long timeLeft = (regenDelay - timeSinceLastDamage) / 1000;
                player.sendMessage("§7Regeneration in: §e" + timeLeft + "s");
            }
        }
        
        return true;
    }
    
    private boolean handleHealth(Player player, Kingdom kingdom) {
        Nexus nexus = kingdom.getNexus();
        
        if (nexus == null) {
            plugin.getMessageManager().sendMessage(player, "nexus.not-found");
            return true;
        }
        
        // Quick health display
        double healthPercent = (double) nexus.getCurrentHealth() / nexus.getMaxHealth() * 100;
        String status = nexus.getStatus().getDisplayName();
        
        player.sendMessage("§6Nexus Health: §c" + nexus.getCurrentHealth() + "§7/§a" + nexus.getMaxHealth() + 
                          " §7(" + String.format("%.1f", healthPercent) + "%)");
        player.sendMessage("§7Status: §e" + status);
        
        return true;
    }
    
    private void sendHelpMessage(Player player) {
        player.sendMessage("§6§l=== Nexus Commands ===");
        player.sendMessage("§e/f nexus place §7- Place your kingdom's nexus (Leader only)");
        player.sendMessage("§e/f nexus remove §7- Remove your kingdom's nexus (Leader only)");
        player.sendMessage("§e/f nexus info §7- Show detailed nexus information");
        player.sendMessage("§e/f nexus health §7- Show nexus health status");
        player.sendMessage("");
        player.sendMessage("§7§lAbout Nexus:");
        player.sendMessage("§7- The nexus is your kingdom's heart and can only be damaged during wars");
        player.sendMessage("§7- It's protected from breaking and explosions");
        player.sendMessage("§7- Only enemy kingdoms at war with you can damage it");
        player.sendMessage("§7- If destroyed, your kingdom loses the war");
        player.sendMessage("§7- It regenerates health over time when not under attack");
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("place", "remove", "info", "health");
        }
        
        return new ArrayList<>();
    }
    
    @Override
    public String getName() {
        return "nexus";
    }
    
    @Override
    public String getDescription() {
        return "Manage your kingdom's nexus";
    }
    
    @Override
    public String getUsage() {
        return "/f nexus <place|remove|info|health>";
    }
    
    @Override
    public boolean hasPermission(Player player) {
        return true; // All players can use nexus commands if they're in a kingdom
    }
}
