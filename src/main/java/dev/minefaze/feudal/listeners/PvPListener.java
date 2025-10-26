package dev.minefaze.feudal.listeners;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.FeudalPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.UUID;

public class PvPListener implements Listener {
    
    private final Feudal plugin;
    
    public PvPListener(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        
        // Get feudal players
        FeudalPlayer attackerData = plugin.getPlayerDataManager().getOrCreatePlayer(attacker);
        FeudalPlayer victimData = plugin.getPlayerDataManager().getOrCreatePlayer(victim);
        
        // If either player doesn't have a kingdom, allow PvP
        if (!attackerData.hasKingdom() || !victimData.hasKingdom()) {
            return;
        }
        
        UUID attackerKingdomId = attackerData.getKingdom().getKingdomId();
        UUID victimKingdomId = victimData.getKingdom().getKingdomId();
        
        // Same kingdom = no PvP
        if (attackerKingdomId.equals(victimKingdomId)) {
            event.setCancelled(true);
            attacker.sendMessage("§c§lNo Friendly Fire! §7You cannot attack your own kingdom members!");
            return;
        }
        
        // Check if they can PvP (not allies or nation members)
        if (!plugin.getAllianceManager().canPvP(attackerKingdomId, victimKingdomId)) {
            event.setCancelled(true);
            
            // Determine relationship type for message
            if (plugin.getAllianceManager().areNationAllies(attackerKingdomId, victimKingdomId)) {
                attacker.sendMessage("§c§lNation Allies! §7You cannot attack members of your nation!");
            } else if (plugin.getAllianceManager().areAllies(attackerKingdomId, victimKingdomId)) {
                attacker.sendMessage("§c§lAllied Kingdom! §7You cannot attack your allies!");
            }
            return;
        }
        
        // Check if they're at war (enemies)
        if (plugin.getAllianceManager().areEnemies(attackerKingdomId, victimKingdomId)) {
            // Apply war damage bonus or other effects
            attacker.sendMessage("§c§lWar Combat! §7You are fighting an enemy kingdom!");
            victim.sendMessage("§c§lUnder Attack! §7You are being attacked by an enemy kingdom!");
        }
    }
}
