package dev.minefaze.feudal.listeners;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TerritoryListener implements Listener {
    
    private final Feudal plugin;
    private final Map<UUID, Chunk> lastPlayerChunk;
    
    public TerritoryListener(Feudal plugin) {
        this.plugin = plugin;
        this.lastPlayerChunk = new HashMap<>();
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Chunk currentChunk = player.getLocation().getChunk();
        
        // Check if player moved to a different chunk
        Chunk lastChunk = lastPlayerChunk.get(player.getUniqueId());
        if (lastChunk != null && lastChunk.equals(currentChunk)) {
            return; // Still in same chunk
        }
        
        // Update last chunk
        lastPlayerChunk.put(player.getUniqueId(), currentChunk);
        
        // Check territory information
        Territory territory = plugin.getKingdomManager().getTerritoryAt(player.getLocation());
        
        if (territory != null) {
            showTerritoryInfo(player, territory);
        } else {
            // Entered wilderness
            showWildernessInfo(player);
        }
    }
    
    private void showTerritoryInfo(Player player, Territory territory) {
        Kingdom kingdom = plugin.getKingdomManager().getKingdom(territory.getKingdomId());
        if (kingdom == null) return;
        
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        UUID playerKingdomId = feudalPlayer.hasKingdom() ? feudalPlayer.getKingdom().getKingdomId() : null;
        
        // Determine relationship and message color
        String relationshipInfo = getRelationshipInfo(territory.getKingdomId(), playerKingdomId);
        String territoryTypeInfo = getTerritoryTypeInfo(territory.getType());
        
        // Send territory notification
        player.sendActionBar("§6§l» §r" + relationshipInfo + kingdom.getName() + " §8| §r" + territoryTypeInfo);
        
        // Show additional info for first entry or important territories
        if (territory.getType() == TerritoryType.CAPITAL || 
            (playerKingdomId != null && plugin.getAllianceManager().areEnemies(playerKingdomId, territory.getKingdomId()))) {
            
            // Delayed message to avoid spam
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && player.getLocation().getChunk().equals(territory.getChunk())) {
                        if (territory.getType() == TerritoryType.CAPITAL) {
                            plugin.getMessageManager().sendMessage(player, "territory.entered-capital", kingdom.getName());
                        } else {
                            plugin.getMessageManager().sendMessage(player, "territory.entered-enemy", kingdom.getName());
                        }
                    }
                }
            }.runTaskLater(plugin, 20L); // 1 second delay
        }
    }
    
    private void showWildernessInfo(Player player) {
        player.sendActionBar("§7§l» §8Wilderness §7- Unclaimed Territory");
    }
    
    private String getRelationshipInfo(UUID territoryOwnerId, UUID playerKingdomId) {
        if (playerKingdomId == null) {
            return "§e"; // Neutral for non-kingdom players
        }
        
        // Own territory
        if (territoryOwnerId.equals(playerKingdomId)) {
            return "§a§l[YOUR KINGDOM] §a";
        }
        
        // Nation ally
        if (plugin.getAllianceManager().areNationAllies(playerKingdomId, territoryOwnerId)) {
            return "§b§l[NATION ALLY] §b";
        }
        
        // Direct ally
        if (plugin.getAllianceManager().areAllies(playerKingdomId, territoryOwnerId)) {
            return "§9§l[ALLY] §9";
        }
        
        // Enemy
        if (plugin.getAllianceManager().areEnemies(playerKingdomId, territoryOwnerId)) {
            return "§c§l[ENEMY] §c";
        }
        
        // Neutral
        return "§e§l[NEUTRAL] §e";
    }
    
    private String getTerritoryTypeInfo(TerritoryType type) {
        return switch (type) {
            case CAPITAL -> "§6§lCapital";
            case OUTPOST -> "§7Outpost";
            case FORTRESS -> "§4§lFortress";
            case TOWN -> "§3§lTown";
            case FARM -> "§2Farm";
            case MINE -> "§8Mine";
        };
    }
    
    /**
     * Clean up player data when they disconnect
     */
    public void onPlayerQuit(UUID playerId) {
        lastPlayerChunk.remove(playerId);
    }
}
