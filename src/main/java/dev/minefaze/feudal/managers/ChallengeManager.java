package dev.minefaze.feudal.managers;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChallengeManager {
    
    private final Feudal plugin;
    private final Map<UUID, Challenge> activeChallenges;
    private final Map<UUID, Set<UUID>> playerChallenges; // playerId -> set of challenge IDs
    
    public ChallengeManager(Feudal plugin) {
        this.plugin = plugin;
        this.activeChallenges = new ConcurrentHashMap<>();
        this.playerChallenges = new ConcurrentHashMap<>();
        
        // Start challenge cleanup task
        startChallengeCleanupTask();
    }
    
    public Challenge createChallenge(UUID challengerId, UUID targetId, ChallengeType type, Territory targetTerritory) {
        FeudalPlayer challenger = plugin.getPlayerDataManager().getPlayer(challengerId);
        FeudalPlayer target = plugin.getPlayerDataManager().getPlayer(targetId);
        
        if (challenger == null || target == null) return null;
        
        // Validation checks
        if (!challenger.canChallenge()) return null;
        if (!target.canChallenge()) return null;
        if (challengerId.equals(targetId)) return null;
        
        // Type-specific validation
        if (type == ChallengeType.LAND_CONQUEST && targetTerritory == null) return null;
        if (type == ChallengeType.LAND_CONQUEST && !targetTerritory.canBeAttacked()) return null;
        
        // Create challenge
        Challenge challenge = new Challenge(challengerId, targetId, type, targetTerritory);
        
        // Register challenge
        activeChallenges.put(challenge.getChallengeId(), challenge);
        playerChallenges.computeIfAbsent(challengerId, k -> new HashSet<>()).add(challenge.getChallengeId());
        playerChallenges.computeIfAbsent(targetId, k -> new HashSet<>()).add(challenge.getChallengeId());
        
        // Update player states
        challenger.setActiveChallenge(challenge);
        target.setActiveChallenge(challenge);
        
        // Mark territory as under attack if applicable
        if (targetTerritory != null) {
            targetTerritory.setUnderAttack(true);
            targetTerritory.setActiveChallenge(challenge);
        }
        
        // Notify players
        notifyChallenge(challenge);
        
        // Save challenge data
        plugin.getDataManager().saveChallengeData(challenge);
        
        return challenge;
    }
    
    public boolean acceptChallenge(UUID challengeId, UUID playerId) {
        Challenge challenge = activeChallenges.get(challengeId);
        if (challenge == null || !challenge.getTarget().equals(playerId)) return false;
        if (challenge.getStatus() != ChallengeStatus.PENDING) return false;
        
        challenge.setStatus(ChallengeStatus.ACCEPTED);
        
        // Check if both players are online to start immediately
        Player challenger = Bukkit.getPlayer(challenge.getChallenger());
        Player target = Bukkit.getPlayer(challenge.getTarget());
        
        if (challenger != null && target != null) {
            startChallenge(challenge);
        } else {
            // Notify that challenge is accepted and waiting for both players to be online
            notifyAcceptedChallenge(challenge);
        }
        
        plugin.getDataManager().saveChallengeData(challenge);
        return true;
    }
    
    public boolean declineChallenge(UUID challengeId, UUID playerId) {
        Challenge challenge = activeChallenges.get(challengeId);
        if (challenge == null || !challenge.getTarget().equals(playerId)) return false;
        if (challenge.getStatus() != ChallengeStatus.PENDING) return false;
        
        challenge.setStatus(ChallengeStatus.DECLINED);
        completeChallenge(challenge, null);
        return true;
    }
    
    public boolean cancelChallenge(UUID challengeId, UUID playerId) {
        Challenge challenge = activeChallenges.get(challengeId);
        if (challenge == null || !challenge.involvesPlayer(playerId)) return false;
        if (challenge.isCompleted()) return false;
        
        challenge.setStatus(ChallengeStatus.CANCELLED);
        completeChallenge(challenge, null);
        return true;
    }
    
    private void startChallenge(Challenge challenge) {
        Player challenger = Bukkit.getPlayer(challenge.getChallenger());
        Player target = Bukkit.getPlayer(challenge.getTarget());
        
        if (challenger == null || target == null) return;
        
        challenge.setStatus(ChallengeStatus.IN_PROGRESS);
        
        // Set battle location (midpoint between players or at territory)
        Location battleLocation;
        if (challenge.getTargetTerritory() != null) {
            battleLocation = challenge.getTargetTerritory().getChunk().getBlock(8, 64, 8).getLocation();
        } else {
            Location loc1 = challenger.getLocation();
            Location loc2 = target.getLocation();
            battleLocation = loc1.add(loc2).multiply(0.5);
        }
        challenge.setBattleLocation(battleLocation);
        
        // Teleport players to battle location
        challenger.teleport(battleLocation);
        target.teleport(battleLocation);
        
        // Mark players as in combat
        FeudalPlayer feudalChallenger = plugin.getPlayerDataManager().getPlayer(challenge.getChallenger());
        FeudalPlayer feudalTarget = plugin.getPlayerDataManager().getPlayer(challenge.getTarget());
        
        if (feudalChallenger != null) feudalChallenger.setInCombat(true);
        if (feudalTarget != null) feudalTarget.setInCombat(true);
        
        // Start combat
        plugin.getCombatManager().startCombat(challenge);
        
        // Notify players
        challenger.sendMessage("§c§lCHALLENGE STARTED! §7Fight for victory!");
        target.sendMessage("§c§lCHALLENGE STARTED! §7Fight for victory!");
        
        plugin.getDataManager().saveChallengeData(challenge);
    }
    
    public void completeChallenge(Challenge challenge, UUID winnerId) {
        if (challenge.isCompleted()) return;
        
        challenge.setStatus(ChallengeStatus.COMPLETED);
        
        // Update player states
        FeudalPlayer challenger = plugin.getPlayerDataManager().getPlayer(challenge.getChallenger());
        FeudalPlayer target = plugin.getPlayerDataManager().getPlayer(challenge.getTarget());
        
        if (challenger != null) {
            challenger.setActiveChallenge(null);
            challenger.setInCombat(false);
        }
        if (target != null) {
            target.setActiveChallenge(null);
            target.setInCombat(false);
        }
        
        // Handle territory transfer if applicable
        if (winnerId != null && challenge.getType() == ChallengeType.LAND_CONQUEST && challenge.getTargetTerritory() != null) {
            handleTerritoryConquest(challenge, winnerId);
        }
        
        // Clear territory attack status
        if (challenge.getTargetTerritory() != null) {
            challenge.getTargetTerritory().setUnderAttack(false);
            challenge.getTargetTerritory().setActiveChallenge(null);
        }
        
        // Remove from active challenges
        activeChallenges.remove(challenge.getChallengeId());
        playerChallenges.getOrDefault(challenge.getChallenger(), new HashSet<>()).remove(challenge.getChallengeId());
        playerChallenges.getOrDefault(challenge.getTarget(), new HashSet<>()).remove(challenge.getChallengeId());
        
        // Notify completion
        notifyChallengeComplete(challenge, winnerId);
        
        // Save final state
        plugin.getDataManager().saveChallengeData(challenge);
    }
    
    private void handleTerritoryConquest(Challenge challenge, UUID winnerId) {
        Territory territory = challenge.getTargetTerritory();
        if (territory == null) return;
        
        FeudalPlayer winner = plugin.getPlayerDataManager().getPlayer(winnerId);
        if (winner == null || !winner.hasKingdom()) return;
        
        // Transfer territory to winner's kingdom
        plugin.getKingdomManager().transferTerritory(territory, winner.getKingdom().getKingdomId());
        
        // Notify about territory conquest
        Player winnerPlayer = Bukkit.getPlayer(winnerId);
        if (winnerPlayer != null) {
            winnerPlayer.sendMessage("§6§lVICTORY! §7You have conquered " + territory.getCoordinates() + "!");
        }
    }
    
    public void handlePlayerLogin(UUID playerId) {
        // Check if player has any accepted challenges waiting
        Set<UUID> challengeIds = playerChallenges.getOrDefault(playerId, new HashSet<>());
        for (UUID challengeId : challengeIds) {
            Challenge challenge = activeChallenges.get(challengeId);
            if (challenge != null && challenge.getStatus() == ChallengeStatus.ACCEPTED) {
                // Check if both players are now online
                Player challenger = Bukkit.getPlayer(challenge.getChallenger());
                Player target = Bukkit.getPlayer(challenge.getTarget());
                
                if (challenger != null && target != null) {
                    // Start the challenge
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            startChallenge(challenge);
                        }
                    }.runTaskLater(plugin, 60L); // 3 second delay
                }
            }
        }
    }
    
    public void handlePlayerLogout(UUID playerId) {
        // Handle active challenges when player logs out
        Set<UUID> challengeIds = new HashSet<>(playerChallenges.getOrDefault(playerId, new HashSet<>()));
        for (UUID challengeId : challengeIds) {
            Challenge challenge = activeChallenges.get(challengeId);
            if (challenge != null && challenge.getStatus() == ChallengeStatus.IN_PROGRESS) {
                // Player loses by forfeit
                UUID opponentId = challenge.getOpponent(playerId);
                completeChallenge(challenge, opponentId);
                
                Player opponent = Bukkit.getPlayer(opponentId);
                if (opponent != null) {
                    opponent.sendMessage("§6§lVICTORY! §7Your opponent has disconnected and forfeited the challenge!");
                }
            }
        }
    }
    
    public void cancelAllChallenges() {
        for (Challenge challenge : new ArrayList<>(activeChallenges.values())) {
            challenge.setStatus(ChallengeStatus.CANCELLED);
            completeChallenge(challenge, null);
        }
    }
    
    private void startChallengeCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredChallenges();
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // Run every minute
    }
    
    private void cleanupExpiredChallenges() {
        List<Challenge> expiredChallenges = activeChallenges.values().stream()
            .filter(Challenge::isExpired)
            .filter(c -> c.getStatus() == ChallengeStatus.PENDING)
            .toList();
        
        for (Challenge challenge : expiredChallenges) {
            challenge.setStatus(ChallengeStatus.EXPIRED);
            completeChallenge(challenge, null);
        }
    }
    
    private void notifyChallenge(Challenge challenge) {
        Player challenger = Bukkit.getPlayer(challenge.getChallenger());
        Player target = Bukkit.getPlayer(challenge.getTarget());
        
        if (challenger != null) {
            challenger.sendMessage("§6§lCHALLENGE SENT! §7Waiting for " + 
                (target != null ? target.getName() : "target") + " to respond.");
        }
        
        if (target != null) {
            target.sendMessage("§c§lCHALLENGE RECEIVED! §7" + 
                (challenger != null ? challenger.getName() : "Someone") + 
                " has challenged you to " + challenge.getType().getDisplayName() + "!");
            target.sendMessage("§7Use §e/feudal accept " + challenge.getChallengeId() + " §7to accept or §e/feudal decline " + challenge.getChallengeId() + " §7to decline.");
        }
    }
    
    private void notifyAcceptedChallenge(Challenge challenge) {
        Player challenger = Bukkit.getPlayer(challenge.getChallenger());
        Player target = Bukkit.getPlayer(challenge.getTarget());
        
        String message = "§a§lCHALLENGE ACCEPTED! §7Waiting for both players to be online to start the battle.";
        
        if (challenger != null) challenger.sendMessage(message);
        if (target != null) target.sendMessage(message);
    }
    
    private void notifyChallengeComplete(Challenge challenge, UUID winnerId) {
        Player challenger = Bukkit.getPlayer(challenge.getChallenger());
        Player target = Bukkit.getPlayer(challenge.getTarget());
        
        String message;
        if (winnerId != null) {
            Player winner = Bukkit.getPlayer(winnerId);
            String winnerName = winner != null ? winner.getName() : "Unknown";
            message = "§6§lCHALLENGE COMPLETE! §7" + winnerName + " has won the " + challenge.getType().getDisplayName() + "!";
        } else {
            message = "§7§lCHALLENGE ENDED! §7The " + challenge.getType().getDisplayName() + " has been cancelled.";
        }
        
        if (challenger != null) challenger.sendMessage(message);
        if (target != null) target.sendMessage(message);
    }
    
    public Challenge getChallenge(UUID challengeId) {
        return activeChallenges.get(challengeId);
    }
    
    public List<Challenge> getPlayerChallenges(UUID playerId) {
        Set<UUID> challengeIds = playerChallenges.getOrDefault(playerId, new HashSet<>());
        return challengeIds.stream()
            .map(activeChallenges::get)
            .filter(Objects::nonNull)
            .toList();
    }
    
    public List<Challenge> getAllActiveChallenges() {
        return new ArrayList<>(activeChallenges.values());
    }
}
