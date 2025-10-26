package dev.minefaze.feudal.listeners;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class CombatListener implements Listener {
    
    private final Feudal plugin;
    
    public CombatListener(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        
        // Check if this is part of an active challenge
        boolean isChallengeCombat = plugin.getCombatManager().isInCombat(victim.getUniqueId()) &&
                                   plugin.getCombatManager().isInCombat(attacker.getUniqueId());
        
        if (isChallengeCombat) {
            // Apply attribute-based damage modifications
            modifyDamageBasedOnAttributes(event, attacker, victim);
        } else {
            // Check if PvP is allowed in this area
            Territory territory = plugin.getKingdomManager().getTerritoryAt(victim.getLocation());
            if (territory != null) {
                Kingdom kingdom = plugin.getKingdomManager().getKingdom(territory.getKingdomId());
                if (kingdom != null && kingdom.isMember(victim.getUniqueId()) && kingdom.isMember(attacker.getUniqueId())) {
                    // Prevent friendly fire within same kingdom
                    attacker.sendMessage("§c§lFriendly Fire! §7You cannot attack members of your own kingdom!");
                    event.setCancelled(true);
                    return;
                }
            }
            
            // Prevent non-challenge PvP (optional - can be configured)
            FeudalPlayer attackerData = plugin.getPlayerDataManager().getPlayer(attacker.getUniqueId());
            FeudalPlayer victimData = plugin.getPlayerDataManager().getPlayer(victim.getUniqueId());
            
            if (attackerData != null && victimData != null && 
                !attackerData.isInCombat() && !victimData.isInCombat()) {
                attacker.sendMessage("§c§lNo Random PvP! §7Use §e/feudal challenge " + victim.getName() + " honor_duel §7to fight properly!");
                event.setCancelled(true);
                return;
            }
        }
    }
    
    private void modifyDamageBasedOnAttributes(EntityDamageByEntityEvent event, Player attacker, Player victim) {
        FeudalPlayer attackerData = plugin.getPlayerDataManager().getPlayer(attacker.getUniqueId());
        FeudalPlayer victimData = plugin.getPlayerDataManager().getPlayer(victim.getUniqueId());
        
        if (attackerData == null || victimData == null) return;
        
        double baseDamage = event.getDamage();
        
        // Calculate damage modifiers
        int attackerStrength = attackerData.getAttribute(Attribute.STRENGTH);
        int attackerWarriorLevel = attackerData.getProfessionLevel(Profession.WARRIOR);
        int victimDefense = victimData.getAttribute(Attribute.DEFENSE);
        int victimWarriorLevel = victimData.getProfessionLevel(Profession.WARRIOR);
        
        // Strength increases damage (10% per point above 10)
        double strengthMultiplier = 1.0 + ((attackerStrength - 10) * 0.1);
        
        // Warrior profession increases damage (5% per level above 1)
        double warriorAttackMultiplier = 1.0 + ((attackerWarriorLevel - 1) * 0.05);
        
        // Defense reduces damage (8% per point above 10)
        double defenseMultiplier = Math.max(0.1, 1.0 - ((victimDefense - 10) * 0.08));
        
        // Warrior profession reduces damage taken (3% per level above 1)
        double warriorDefenseMultiplier = Math.max(0.1, 1.0 - ((victimWarriorLevel - 1) * 0.03));
        
        // Apply all modifiers
        double finalDamage = baseDamage * strengthMultiplier * warriorAttackMultiplier * defenseMultiplier * warriorDefenseMultiplier;
        
        event.setDamage(Math.max(0.5, finalDamage)); // Minimum 0.5 damage
        
        // Show damage numbers to players
        attacker.sendMessage("§c⚔ §7Dealt §c" + String.format("%.1f", finalDamage) + " §7damage!");
        victim.sendMessage("§c❤ §7Took §c" + String.format("%.1f", finalDamage) + " §7damage!");
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        // Handle challenge combat deaths
        if (plugin.getCombatManager().isInCombat(victim.getUniqueId())) {
            plugin.getCombatManager().handlePlayerDeath(victim, killer);
            
            // Modify death message for challenge combat
            if (killer != null && plugin.getCombatManager().isInCombat(killer.getUniqueId())) {
                event.setDeathMessage("§6§l" + killer.getName() + " §7has defeated §6§l" + victim.getName() + " §7in honorable combat!");
            }
        }
        
        // Award experience to killer if it was a valid PvP kill
        if (killer != null && killer != victim) {
            FeudalPlayer killerData = plugin.getPlayerDataManager().getPlayer(killer.getUniqueId());
            if (killerData != null) {
                // Award warrior experience for PvP kills
                plugin.getPlayerDataManager().addExperience(killer.getUniqueId(), Profession.WARRIOR, 25);
                killer.sendMessage("§6§lPvP Kill! §7You gained 25 Warrior experience!");
            }
        }
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        
        if (feudalPlayer != null && feudalPlayer.hasKingdom()) {
            Kingdom kingdom = feudalPlayer.getKingdom();
            if (kingdom.getCapital() != null) {
                // Respawn at kingdom capital if available
                event.setRespawnLocation(kingdom.getCapital());
                player.sendMessage("§a§lRespawned! §7You have returned to your kingdom's capital.");
            }
        }
    }
}
