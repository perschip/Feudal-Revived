package dev.minefaze.feudal.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.stream.Collectors;

public class Kingdom {
    
    public enum JoinType {
        OPEN("Open", "Anyone can join"),
        CLOSED("Closed", "No one can join"),
        INVITE_ONLY("Invite Only", "Only invited players can join");
        
        private final String displayName;
        private final String description;
        
        JoinType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    private final UUID kingdomId;
    private String name;
    private UUID leader;
    private Set<UUID> members;
    private Set<Territory> territories;
    private Location capital;
    private int treasury;
    private long creationTime;
    private Map<String, Object> settings;
    private TownHall townHall;
    private Nexus nexus;
    private JoinType joinType;
    
    public Kingdom(UUID kingdomId, String name, UUID leader, Location capital) {
        this.kingdomId = kingdomId;
        this.name = name;
        this.leader = leader;
        this.capital = capital;
        this.members = new HashSet<>();
        this.territories = new HashSet<>();
        this.treasury = 0;
        this.creationTime = System.currentTimeMillis();
        this.settings = new HashMap<>();
        this.joinType = JoinType.OPEN; // Default to open
        
        // Add leader as first member
        members.add(leader);
        
        // Create initial territory around capital
        if (capital != null) {
            Territory capitalTerritory = new Territory(
                UUID.randomUUID(),
                this.kingdomId,
                capital.getChunk(),
                TerritoryType.CAPITAL
            );
            territories.add(capitalTerritory);
        }
    }
    
    // Getters and Setters
    public UUID getKingdomId() { return kingdomId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }
    
    public Set<UUID> getMembers() { return new HashSet<>(members); }
    public void addMember(UUID playerId) { members.add(playerId); }
    public void removeMember(UUID playerId) { members.remove(playerId); }
    public boolean isMember(UUID playerId) { return members.contains(playerId); }
    public int getMemberCount() { return members.size(); }
    
    /**
     * Get all online members of this kingdom
     * @return List of online Player objects who are members of this kingdom
     */
    public List<Player> getOnlineMembers() {
        return members.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    public Set<Territory> getTerritories() { return new HashSet<>(territories); }
    public void addTerritory(Territory territory) { territories.add(territory); }
    public void removeTerritory(Territory territory) { territories.remove(territory); }
    public int getTerritoryCount() { return territories.size(); }
    
    public Location getCapital() { return capital; }
    public void setCapital(Location capital) { this.capital = capital; }
    
    public int getTreasury() { return treasury; }
    public void setTreasury(int treasury) { this.treasury = treasury; }
    public void addToTreasury(int amount) { this.treasury += amount; }
    public boolean removeFromTreasury(int amount) {
        if (treasury >= amount) {
            treasury -= amount;
            return true;
        }
        return false;
    }
    
    public long getCreationTime() { return creationTime; }
    public Map<String, Object> getSettings() { return settings; }
    
    public JoinType getJoinType() { return joinType; }
    public void setJoinType(JoinType joinType) { this.joinType = joinType; }
    
    // Utility methods
    public boolean isLeader(UUID playerId) { return leader.equals(playerId); }
    
    public Territory getTerritoryAt(org.bukkit.Chunk chunk) {
        return territories.stream()
                .filter(t -> t.getChunk().equals(chunk))
                .findFirst()
                .orElse(null);
    }
    
    public boolean ownsTerritory(org.bukkit.Chunk chunk) {
        return getTerritoryAt(chunk) != null;
    }
    
    public List<Territory> getTerritoriesByType(TerritoryType type) {
        return territories.stream()
                .filter(t -> t.getType() == type)
                .toList();
    }
    
    public int getPowerLevel() {
        // Kingdom power based on members, territories, and treasury
        int memberPower = members.size() * 10;
        int territoryPower = territories.size() * 5;
        int treasuryPower = treasury / 100;
        return memberPower + territoryPower + treasuryPower;
    }
    
    public boolean canExpand() {
        // Kingdoms can expand based on town hall level
        if (townHall != null) {
            return territories.size() < townHall.getMaxTerritories();
        }
        // Fallback for kingdoms without town hall
        int maxTerritories = members.size() * 3; // 3 territories per member
        return territories.size() < maxTerritories;
    }
    
    // TownHall and Nexus getters/setters
    public TownHall getTownHall() { return townHall; }
    public void setTownHall(TownHall townHall) { 
        this.townHall = townHall;
        // Update nexus when town hall changes
        if (nexus != null && townHall != null) {
            nexus.updateForTownHallLevel(townHall.getLevel());
        }
    }
    
    public Nexus getNexus() { return nexus; }
    public void setNexus(Nexus nexus) { this.nexus = nexus; }
    
    /**
     * Initialize town hall and nexus for new kingdom
     */
    public void initializeTownHallAndNexus(TownHall.TownHallType type) {
        if (capital != null) {
            this.townHall = new TownHall(kingdomId, capital, type);
            this.nexus = new Nexus(kingdomId, 1);
        }
    }
    
    /**
     * Get maximum members based on town hall level
     */
    public int getMaxMembers() {
        if (townHall != null) {
            return townHall.getMaxMembers();
        }
        return 5; // Default for kingdoms without town hall
    }
    
    /**
     * Check if kingdom can accept new members
     */
    public boolean canAcceptNewMembers() {
        return members.size() < getMaxMembers();
    }
    
    /**
     * Get town hall level
     */
    public int getTownHallLevel() {
        return townHall != null ? townHall.getLevel() : 1;
    }
}
