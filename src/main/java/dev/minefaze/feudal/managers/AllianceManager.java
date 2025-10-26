package dev.minefaze.feudal.managers;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class AllianceManager {
    
    private final Feudal plugin;
    private final Map<UUID, Alliance> alliances;
    private final Map<UUID, Nation> nations;
    private final Map<UUID, UUID> kingdomToNation; // Kingdom ID -> Nation ID
    
    public AllianceManager(Feudal plugin) {
        this.plugin = plugin;
        this.alliances = new HashMap<>();
        this.nations = new HashMap<>();
        this.kingdomToNation = new HashMap<>();
    }
    
    // Alliance Management
    public Alliance createAlliance(UUID kingdom1Id, UUID kingdom2Id, Alliance.AllianceType type) {
        // Check if alliance already exists
        Alliance existing = getAlliance(kingdom1Id, kingdom2Id);
        if (existing != null) {
            existing.setType(type);
            existing.setActive(true);
            return existing;
        }
        
        Alliance alliance = new Alliance(kingdom1Id, kingdom2Id, type);
        alliances.put(alliance.getAllianceId(), alliance);
        
        // Notify kingdoms
        notifyKingdoms(alliance, "Alliance " + type.getDisplayName() + " established!");
        
        return alliance;
    }
    
    public boolean removeAlliance(UUID kingdom1Id, UUID kingdom2Id) {
        Alliance alliance = getAlliance(kingdom1Id, kingdom2Id);
        if (alliance != null) {
            alliance.setActive(false);
            notifyKingdoms(alliance, "Alliance dissolved!");
            return true;
        }
        return false;
    }
    
    public Alliance getAlliance(UUID kingdom1Id, UUID kingdom2Id) {
        return alliances.values().stream()
            .filter(alliance -> alliance.involvesKingdom(kingdom1Id) && alliance.involvesKingdom(kingdom2Id))
            .filter(Alliance::isActive)
            .findFirst()
            .orElse(null);
    }
    
    public List<Alliance> getKingdomAlliances(UUID kingdomId) {
        return alliances.values().stream()
            .filter(alliance -> alliance.involvesKingdom(kingdomId))
            .filter(Alliance::isActive)
            .collect(Collectors.toList());
    }
    
    public boolean areAllies(UUID kingdom1Id, UUID kingdom2Id) {
        Alliance alliance = getAlliance(kingdom1Id, kingdom2Id);
        return alliance != null && alliance.isAlly();
    }
    
    public boolean areEnemies(UUID kingdom1Id, UUID kingdom2Id) {
        Alliance alliance = getAlliance(kingdom1Id, kingdom2Id);
        return alliance != null && alliance.isEnemy();
    }
    
    // Nation Management
    public Nation createNation(String name, UUID leaderKingdomId) {
        // Check if kingdom is already in a nation
        if (kingdomToNation.containsKey(leaderKingdomId)) {
            return null;
        }
        
        Nation nation = new Nation(name, leaderKingdomId);
        nations.put(nation.getNationId(), nation);
        kingdomToNation.put(leaderKingdomId, nation.getNationId());
        
        // Notify kingdom
        Kingdom kingdom = plugin.getKingdomManager().getKingdom(leaderKingdomId);
        if (kingdom != null) {
            notifyKingdom(kingdom, "§6§lNation Created! §7" + name + " has been established!");
        }
        
        return nation;
    }
    
    public boolean joinNation(UUID nationId, UUID kingdomId) {
        Nation nation = nations.get(nationId);
        if (nation == null || !nation.isActive()) return false;
        
        // Check if kingdom is already in a nation
        if (kingdomToNation.containsKey(kingdomId)) return false;
        
        if (nation.addKingdom(kingdomId)) {
            kingdomToNation.put(kingdomId, nationId);
            
            // Notify both kingdoms
            Kingdom joiningKingdom = plugin.getKingdomManager().getKingdom(kingdomId);
            if (joiningKingdom != null) {
                notifyKingdom(joiningKingdom, "§a§lJoined Nation! §7Welcome to " + nation.getName() + "!");
            }
            
            notifyNation(nation, joiningKingdom.getName() + " has joined the nation!");
            return true;
        }
        return false;
    }
    
    public boolean leaveNation(UUID kingdomId) {
        UUID nationId = kingdomToNation.get(kingdomId);
        if (nationId == null) return false;
        
        Nation nation = nations.get(nationId);
        if (nation == null) return false;
        
        // Cannot leave if you're the leader and there are other members
        if (nation.isLeader(kingdomId) && nation.getMemberCount() > 1) {
            return false;
        }
        
        if (nation.removeKingdom(kingdomId)) {
            kingdomToNation.remove(kingdomId);
            
            Kingdom kingdom = plugin.getKingdomManager().getKingdom(kingdomId);
            if (kingdom != null) {
                notifyKingdom(kingdom, "§e§lLeft Nation! §7You have left " + nation.getName());
            }
            
            // If leader left and was the last member, dissolve nation
            if (nation.getMemberCount() == 0) {
                dissolveNation(nationId);
            }
            
            return true;
        }
        return false;
    }
    
    public void dissolveNation(UUID nationId) {
        Nation nation = nations.get(nationId);
        if (nation == null) return;
        
        // Remove all kingdoms from nation mapping
        for (UUID kingdomId : nation.getMemberKingdomIds()) {
            kingdomToNation.remove(kingdomId);
        }
        
        notifyNation(nation, "§c§lNation Dissolved! §7" + nation.getName() + " has been disbanded.");
        
        nation.setActive(false);
        nations.remove(nationId);
    }
    
    // Utility Methods
    public Nation getKingdomNation(UUID kingdomId) {
        UUID nationId = kingdomToNation.get(kingdomId);
        return nationId != null ? nations.get(nationId) : null;
    }
    
    public boolean areNationAllies(UUID kingdom1Id, UUID kingdom2Id) {
        Nation nation1 = getKingdomNation(kingdom1Id);
        Nation nation2 = getKingdomNation(kingdom2Id);
        
        // Same nation = allies
        if (nation1 != null && nation2 != null && nation1.equals(nation2)) {
            return true;
        }
        
        // Check direct alliance
        return areAllies(kingdom1Id, kingdom2Id);
    }
    
    public boolean canPvP(UUID kingdom1Id, UUID kingdom2Id) {
        // Cannot PvP with nation allies
        if (areNationAllies(kingdom1Id, kingdom2Id)) return false;
        
        // Cannot PvP with direct allies
        if (areAllies(kingdom1Id, kingdom2Id)) return false;
        
        return true;
    }
    
    public boolean areAtWar(Kingdom kingdom1, Kingdom kingdom2) {
        if (kingdom1 == null || kingdom2 == null) return false;
        
        UUID kingdom1Id = kingdom1.getKingdomId();
        UUID kingdom2Id = kingdom2.getKingdomId();
        
        // Cannot be at war with yourself
        if (kingdom1Id.equals(kingdom2Id)) return false;
        
        // At war if they are enemies OR if they can PvP (not allies)
        return areEnemies(kingdom1Id, kingdom2Id) || canPvP(kingdom1Id, kingdom2Id);
    }
    
    public boolean canInteractInTerritory(UUID kingdomId, UUID territoryOwnerId) {
        // Own territory
        if (kingdomId.equals(territoryOwnerId)) return true;
        
        // Nation allies can interact
        if (areNationAllies(kingdomId, territoryOwnerId)) return true;
        
        // Direct allies can interact
        if (areAllies(kingdomId, territoryOwnerId)) return true;
        
        return false;
    }
    
    public String getRelationshipType(Kingdom kingdom1, Kingdom kingdom2) {
        if (kingdom1 == null || kingdom2 == null) return "NEUTRAL";
        
        UUID kingdom1Id = kingdom1.getKingdomId();
        UUID kingdom2Id = kingdom2.getKingdomId();
        
        // Check if they're the same kingdom
        if (kingdom1Id.equals(kingdom2Id)) return "OWN";
        
        // Check if they're nation allies (same nation or allied nations)
        if (areNationAllies(kingdom1Id, kingdom2Id)) return "ALLY";
        
        // Check direct alliance
        Alliance alliance = getAlliance(kingdom1Id, kingdom2Id);
        if (alliance != null && alliance.isActive()) {
            return alliance.getType().name();
        }
        
        // Default to neutral
        return "NEUTRAL";
    }
    
    // Notification helpers
    private void notifyKingdoms(Alliance alliance, String message) {
        Kingdom kingdom1 = plugin.getKingdomManager().getKingdom(alliance.getKingdom1Id());
        Kingdom kingdom2 = plugin.getKingdomManager().getKingdom(alliance.getKingdom2Id());
        
        if (kingdom1 != null) notifyKingdom(kingdom1, message);
        if (kingdom2 != null) notifyKingdom(kingdom2, message);
    }
    
    private void notifyKingdom(Kingdom kingdom, String message) {
        for (UUID memberId : kingdom.getMembers()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null) {
                player.sendMessage("§6§l[Kingdom] §r" + message);
            }
        }
    }
    
    private void notifyNation(Nation nation, String message) {
        for (UUID kingdomId : nation.getMemberKingdomIds()) {
            Kingdom kingdom = plugin.getKingdomManager().getKingdom(kingdomId);
            if (kingdom != null) {
                notifyKingdom(kingdom, message);
            }
        }
    }
    
    // Getters
    public Collection<Alliance> getAllAlliances() { return alliances.values(); }
    public Collection<Nation> getAllNations() { return nations.values(); }
    public Nation getNation(UUID nationId) { return nations.get(nationId); }
}
