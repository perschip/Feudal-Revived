package dev.minefaze.feudal.models;

import org.bukkit.Chunk;
import java.util.UUID;

public class Territory {
    
    private final UUID territoryId;
    private final UUID kingdomId;
    private final Chunk chunk;
    private final TerritoryType type;
    private final long claimTime;
    private int defenseLevel;
    private boolean underAttack;
    private Challenge activeChallenge;
    
    public Territory(UUID territoryId, UUID kingdomId, Chunk chunk, TerritoryType type) {
        this.territoryId = territoryId;
        this.kingdomId = kingdomId;
        this.chunk = chunk;
        this.type = type;
        this.claimTime = System.currentTimeMillis();
        this.defenseLevel = 1;
        this.underAttack = false;
    }
    
    // Getters and Setters
    public UUID getTerritoryId() { return territoryId; }
    public UUID getKingdomId() { return kingdomId; }
    public Chunk getChunk() { return chunk; }
    public TerritoryType getType() { return type; }
    public long getClaimTime() { return claimTime; }
    
    public int getDefenseLevel() { return defenseLevel; }
    public void setDefenseLevel(int defenseLevel) { this.defenseLevel = defenseLevel; }
    
    public boolean isUnderAttack() { return underAttack; }
    public void setUnderAttack(boolean underAttack) { this.underAttack = underAttack; }
    
    public Challenge getActiveChallenge() { return activeChallenge; }
    public void setActiveChallenge(Challenge activeChallenge) { this.activeChallenge = activeChallenge; }
    
    // Utility methods
    public boolean canBeAttacked() {
        return !underAttack && activeChallenge == null;
    }
    
    public int getDefensePower() {
        int basePower = defenseLevel * 10;
        int typePower = type.getDefenseBonus();
        return basePower + typePower;
    }
    
    public String getCoordinates() {
        return String.format("(%d, %d)", chunk.getX(), chunk.getZ());
    }
    
    public String getWorldName() {
        return chunk.getWorld().getName();
    }
}
