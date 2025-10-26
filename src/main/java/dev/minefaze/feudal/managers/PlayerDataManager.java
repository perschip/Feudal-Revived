package dev.minefaze.feudal.managers;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager implements Listener {
    
    private final Feudal plugin;
    private final Map<UUID, FeudalPlayer> playerData;
    
    public PlayerDataManager(Feudal plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();
    }
    
    public FeudalPlayer getPlayer(UUID playerId) {
        return playerData.get(playerId);
    }
    
    public FeudalPlayer getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }
    
    public FeudalPlayer getOrCreatePlayer(UUID playerId, String playerName) {
        FeudalPlayer feudalPlayer = playerData.get(playerId);
        if (feudalPlayer == null) {
            feudalPlayer = new FeudalPlayer(playerId, playerName);
            playerData.put(playerId, feudalPlayer);
        }
        return feudalPlayer;
    }
    
    public FeudalPlayer getOrCreatePlayer(Player player) {
        return getOrCreatePlayer(player.getUniqueId(), player.getName());
    }
    
    public void savePlayer(FeudalPlayer player) {
        // Save player data to storage
        plugin.getDataManager().savePlayerData(player);
    }
    
    public void loadPlayer(UUID playerId) {
        FeudalPlayer player = plugin.getDataManager().loadPlayerData(playerId);
        if (player != null) {
            playerData.put(playerId, player);
        }
    }
    
    public void unloadPlayer(UUID playerId) {
        FeudalPlayer player = playerData.get(playerId);
        if (player != null) {
            savePlayer(player);
            playerData.remove(playerId);
        }
    }
    
    public boolean hasPlayerData(UUID playerId) {
        return playerData.containsKey(playerId);
    }
    
    public void addExperience(UUID playerId, Profession profession, int experience) {
        FeudalPlayer player = getPlayer(playerId);
        if (player != null) {
            player.addExperience(experience);
            
            // Calculate profession level up
            int currentLevel = player.getProfessionLevel(profession);
            int experienceForNext = getExperienceForLevel(currentLevel + 1);
            
            if (player.getTotalExperience() >= experienceForNext) {
                player.setProfessionLevel(profession, currentLevel + 1);
                
                // Notify player of level up
                Player bukkitPlayer = plugin.getServer().getPlayer(playerId);
                if (bukkitPlayer != null) {
                    bukkitPlayer.sendMessage("§6Congratulations! Your " + profession.getDisplayName() + 
                                           " profession has reached level " + (currentLevel + 1) + "!");
                }
            }
        }
    }
    
    public void addAttributePoints(UUID playerId, Attribute attribute, int points) {
        FeudalPlayer player = getPlayer(playerId);
        if (player != null) {
            int currentValue = player.getAttribute(attribute);
            player.setAttribute(attribute, currentValue + points);
        }
    }
    
    public int getExperienceForLevel(int level) {
        // Exponential experience curve
        return (int) (100 * Math.pow(1.5, level - 1));
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Load or create player data
        loadPlayer(playerId);
        if (!hasPlayerData(playerId)) {
            getOrCreatePlayer(player);
        }
        
        // Update player name in case it changed
        FeudalPlayer feudalPlayer = getPlayer(playerId);
        if (feudalPlayer != null) {
            feudalPlayer.setPlayerName(player.getName());
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        
        // Cancel any active challenges
        FeudalPlayer feudalPlayer = getPlayer(playerId);
        if (feudalPlayer != null && feudalPlayer.getActiveChallenge() != null) {
            plugin.getChallengeManager().handlePlayerLogout(playerId);
        }
        
        // Save and unload player data
        unloadPlayer(playerId);
    }
    
    public Map<UUID, FeudalPlayer> getAllPlayers() {
        return new HashMap<>(playerData);
    }
}
