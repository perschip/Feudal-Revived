package dev.minefaze.feudal.models;

import org.bukkit.Location;
import java.util.UUID;

public class Challenge {
    
    private final UUID challengeId;
    private final UUID challenger;
    private final UUID target;
    private final ChallengeType type;
    private final Territory targetTerritory; // For land conquest challenges
    private final long creationTime;
    private final long expirationTime;
    private ChallengeStatus status;
    private Location battleLocation;
    private String reason;
    private int wager; // Amount of money/resources at stake
    
    public Challenge(UUID challenger, UUID target, ChallengeType type, Territory targetTerritory) {
        this.challengeId = UUID.randomUUID();
        this.challenger = challenger;
        this.target = target;
        this.type = type;
        this.targetTerritory = targetTerritory;
        this.creationTime = System.currentTimeMillis();
        this.expirationTime = creationTime + (24 * 60 * 60 * 1000); // 24 hours
        this.status = ChallengeStatus.PENDING;
        this.wager = 0;
    }
    
    // Getters and Setters
    public UUID getChallengeId() { return challengeId; }
    public UUID getChallenger() { return challenger; }
    public UUID getTarget() { return target; }
    public ChallengeType getType() { return type; }
    public Territory getTargetTerritory() { return targetTerritory; }
    public long getCreationTime() { return creationTime; }
    public long getExpirationTime() { return expirationTime; }
    
    public ChallengeStatus getStatus() { return status; }
    public void setStatus(ChallengeStatus status) { this.status = status; }
    
    public Location getBattleLocation() { return battleLocation; }
    public void setBattleLocation(Location battleLocation) { this.battleLocation = battleLocation; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public int getWager() { return wager; }
    public void setWager(int wager) { this.wager = wager; }
    
    // Utility methods
    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }
    
    public boolean isPending() {
        return status == ChallengeStatus.PENDING;
    }
    
    public boolean isActive() {
        return status == ChallengeStatus.ACCEPTED || status == ChallengeStatus.IN_PROGRESS;
    }
    
    public boolean isCompleted() {
        return status == ChallengeStatus.COMPLETED || 
               status == ChallengeStatus.CANCELLED ||
               status == ChallengeStatus.EXPIRED;
    }
    
    public long getTimeRemaining() {
        return Math.max(0, expirationTime - System.currentTimeMillis());
    }
    
    public String getTimeRemainingFormatted() {
        long remaining = getTimeRemaining();
        if (remaining <= 0) return "Expired";
        
        long hours = remaining / (60 * 60 * 1000);
        long minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000);
        
        return String.format("%d hours, %d minutes", hours, minutes);
    }
    
    public boolean involvesPlayer(UUID playerId) {
        return challenger.equals(playerId) || target.equals(playerId);
    }
    
    public UUID getOpponent(UUID playerId) {
        if (challenger.equals(playerId)) return target;
        if (target.equals(playerId)) return challenger;
        return null;
    }
}
