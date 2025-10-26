package dev.minefaze.feudal.managers;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager {
    
    private final Feudal plugin;
    private final Map<UUID, CombatSession> activeCombatSessions;
    
    public CombatManager(Feudal plugin) {
        this.plugin = plugin;
        this.activeCombatSessions = new HashMap<>();
    }
    
    public void startCombat(Challenge challenge) {
        UUID challengerId = challenge.getChallenger();
        UUID targetId = challenge.getTarget();
        
        Player challenger = Bukkit.getPlayer(challengerId);
        Player target = Bukkit.getPlayer(targetId);
        
        if (challenger == null || target == null) return;
        
        // Create combat session
        CombatSession session = new CombatSession(challenge, challengerId, targetId);
        activeCombatSessions.put(challenge.getChallengeId(), session);
        
        // Apply combat effects based on player attributes
        applyCombatEffects(challenger, challengerId);
        applyCombatEffects(target, targetId);
        
        // Start combat timer
        startCombatTimer(session);
        
        // Notify players of combat start
        challenger.sendMessage("§c§lCOMBAT STARTED! §7Fight with honor!");
        target.sendMessage("§c§lCOMBAT STARTED! §7Fight with honor!");
        
        // Broadcast to nearby players
        broadcastCombatStart(challenge);
    }
    
    private void applyCombatEffects(Player player, UUID playerId) {
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getPlayer(playerId);
        if (feudalPlayer == null) return;
        
        // Apply attribute-based effects
        int strength = feudalPlayer.getAttribute(Attribute.STRENGTH);
        int defense = feudalPlayer.getAttribute(Attribute.DEFENSE);
        int agility = feudalPlayer.getAttribute(Attribute.AGILITY);
        int endurance = feudalPlayer.getAttribute(Attribute.ENDURANCE);
        
        // Strength increases damage (handled in damage calculation)
        // Defense reduces damage taken (handled in damage calculation)
        
        // Agility increases speed
        if (agility > 15) {
            int speedLevel = Math.min((agility - 15) / 5, 2);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, speedLevel));
        }
        
        // Endurance increases health
        if (endurance > 15) {
            int healthBoost = Math.min((endurance - 15) / 3, 5);
            player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, Integer.MAX_VALUE, healthBoost));
        }
        
        // Profession bonuses
        int warriorLevel = feudalPlayer.getProfessionLevel(Profession.WARRIOR);
        if (warriorLevel > 5) {
            // Warriors get resistance at higher levels
            int resistanceLevel = Math.min((warriorLevel - 5) / 10, 1);
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, resistanceLevel));
        }
    }
    
    public void handlePlayerDeath(Player player, Player killer) {
        UUID playerId = player.getUniqueId();
        UUID killerId = killer != null ? killer.getUniqueId() : null;
        
        // Find active combat session
        CombatSession session = findCombatSession(playerId);
        if (session == null) return;
        
        // Determine winner
        UUID winnerId = session.getOpponent(playerId);
        if (winnerId != null && winnerId.equals(killerId)) {
            // Valid combat kill
            endCombat(session, winnerId);
        } else {
            // Invalid kill or environmental death
            endCombat(session, null);
        }
    }
    
    public void handlePlayerLogout(UUID playerId) {
        CombatSession session = findCombatSession(playerId);
        if (session != null) {
            // Player forfeits by logging out
            UUID winnerId = session.getOpponent(playerId);
            endCombat(session, winnerId);
        }
    }
    
    private void endCombat(CombatSession session, UUID winnerId) {
        Challenge challenge = session.getChallenge();
        
        // Remove combat effects
        Player player1 = Bukkit.getPlayer(session.getPlayer1());
        Player player2 = Bukkit.getPlayer(session.getPlayer2());
        
        if (player1 != null) removeCombatEffects(player1);
        if (player2 != null) removeCombatEffects(player2);
        
        // Award experience and rewards
        if (winnerId != null) {
            awardCombatRewards(winnerId, session.getOpponent(winnerId));
        }
        
        // Complete the challenge
        plugin.getChallengeManager().completeChallenge(challenge, winnerId);
        
        // Remove combat session
        activeCombatSessions.remove(challenge.getChallengeId());
        
        // Broadcast combat end
        broadcastCombatEnd(challenge, winnerId);
    }
    
    private void removeCombatEffects(Player player) {
        // Remove all combat-related potion effects
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.STRENGTH);
    }
    
    private void awardCombatRewards(UUID winnerId, UUID loserId) {
        FeudalPlayer winner = plugin.getPlayerDataManager().getPlayer(winnerId);
        FeudalPlayer loser = plugin.getPlayerDataManager().getPlayer(loserId);
        
        if (winner == null) return;
        
        // Award experience to winner
        int baseExperience = 50;
        int levelDifference = 0;
        
        if (loser != null) {
            int winnerLevel = winner.getProfessionLevel(Profession.WARRIOR);
            int loserLevel = loser.getProfessionLevel(Profession.WARRIOR);
            levelDifference = Math.max(0, loserLevel - winnerLevel);
        }
        
        int totalExperience = baseExperience + (levelDifference * 10);
        plugin.getPlayerDataManager().addExperience(winnerId, Profession.WARRIOR, totalExperience);
        
        // Award attribute points
        plugin.getPlayerDataManager().addAttributePoints(winnerId, Attribute.STRENGTH, 1);
        
        // Notify winner
        Player winnerPlayer = Bukkit.getPlayer(winnerId);
        if (winnerPlayer != null) {
            winnerPlayer.sendMessage("§6§lVICTORY! §7You gained " + totalExperience + " Warrior experience and 1 Strength point!");
        }
        
        // Small consolation for loser
        if (loser != null) {
            plugin.getPlayerDataManager().addExperience(loserId, Profession.WARRIOR, 10);
            Player loserPlayer = Bukkit.getPlayer(loserId);
            if (loserPlayer != null) {
                loserPlayer.sendMessage("§c§lDEFEAT! §7You gained 10 Warrior experience for participating in combat.");
            }
        }
    }
    
    private void startCombatTimer(CombatSession session) {
        new BukkitRunnable() {
            int timeLeft = 300; // 5 minutes
            
            @Override
            public void run() {
                if (!activeCombatSessions.containsKey(session.getChallenge().getChallengeId())) {
                    cancel();
                    return;
                }
                
                timeLeft--;
                
                // Warn players at certain intervals
                if (timeLeft == 60 || timeLeft == 30 || timeLeft == 10) {
                    Player player1 = Bukkit.getPlayer(session.getPlayer1());
                    Player player2 = Bukkit.getPlayer(session.getPlayer2());
                    
                    String message = "§e§lCOMBAT WARNING! §7" + timeLeft + " seconds remaining!";
                    if (player1 != null) player1.sendMessage(message);
                    if (player2 != null) player2.sendMessage(message);
                }
                
                // End combat if time runs out
                if (timeLeft <= 0) {
                    endCombat(session, null); // Draw
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    private CombatSession findCombatSession(UUID playerId) {
        return activeCombatSessions.values().stream()
            .filter(session -> session.involvesPlayer(playerId))
            .findFirst()
            .orElse(null);
    }
    
    private void broadcastCombatStart(Challenge challenge) {
        Player challenger = Bukkit.getPlayer(challenge.getChallenger());
        Player target = Bukkit.getPlayer(challenge.getTarget());
        
        if (challenger == null || target == null) return;
        
        String message = "§6§lCOMBAT! §7" + challenger.getName() + " §7vs §7" + target.getName() + 
                        " §7- " + challenge.getType().getDisplayName();
        
        // Broadcast to all online players
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (!player.equals(challenger) && !player.equals(target)) {
                player.sendMessage(message);
            }
        });
    }
    
    private void broadcastCombatEnd(Challenge challenge, UUID winnerId) {
        Player challenger = Bukkit.getPlayer(challenge.getChallenger());
        Player target = Bukkit.getPlayer(challenge.getTarget());
        Player winner = winnerId != null ? Bukkit.getPlayer(winnerId) : null;
        
        String message;
        if (winner != null) {
            message = "§6§lCOMBAT ENDED! §7" + winner.getName() + " §7has won the " + 
                     challenge.getType().getDisplayName() + "!";
        } else {
            message = "§7§lCOMBAT ENDED! §7The " + challenge.getType().getDisplayName() + " ended in a draw.";
        }
        
        // Broadcast to all online players
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
    }
    
    public boolean isInCombat(UUID playerId) {
        return findCombatSession(playerId) != null;
    }
    
    public CombatSession getCombatSession(UUID playerId) {
        return findCombatSession(playerId);
    }
    
    // Inner class for combat sessions
    private static class CombatSession {
        private final Challenge challenge;
        private final UUID player1;
        private final UUID player2;
        private final long startTime;
        
        public CombatSession(Challenge challenge, UUID player1, UUID player2) {
            this.challenge = challenge;
            this.player1 = player1;
            this.player2 = player2;
            this.startTime = System.currentTimeMillis();
        }
        
        public Challenge getChallenge() { return challenge; }
        public UUID getPlayer1() { return player1; }
        public UUID getPlayer2() { return player2; }
        public long getStartTime() { return startTime; }
        
        public boolean involvesPlayer(UUID playerId) {
            return player1.equals(playerId) || player2.equals(playerId);
        }
        
        public UUID getOpponent(UUID playerId) {
            if (player1.equals(playerId)) return player2;
            if (player2.equals(playerId)) return player1;
            return null;
        }
    }
}
