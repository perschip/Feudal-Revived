package dev.minefaze.feudal.listeners;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.models.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerListener implements Listener {
    
    private final Feudal plugin;
    
    public PlayerListener(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Handle challenge system login
        plugin.getChallengeManager().handlePlayerLogin(player.getUniqueId());
        
        // Welcome message for new players
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (feudalPlayer == null) {
            plugin.getMessageManager().sendMessage(player, "welcome.header");
            plugin.getMessageManager().sendMessage(player, "welcome.description");
            plugin.getMessageManager().sendMessage(player, "welcome.help-tip");
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Handle challenge system logout
        plugin.getChallengeManager().handlePlayerLogout(player.getUniqueId());
        
        // Handle combat system logout
        plugin.getCombatManager().handlePlayerLogout(player.getUniqueId());
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Check territory permissions with alliance support
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        Territory territory = plugin.getKingdomManager().getTerritoryAt(block.getLocation());
        
        if (territory != null && feudalPlayer.hasKingdom()) {
            UUID playerKingdomId = feudalPlayer.getKingdom().getKingdomId();
            UUID territoryOwnerId = territory.getKingdomId();
            
            // Check if player can interact in this territory (own, ally, or nation member)
            if (!plugin.getAllianceManager().canInteractInTerritory(playerKingdomId, territoryOwnerId)) {
                Kingdom kingdom = plugin.getKingdomManager().getKingdom(territoryOwnerId);
                String kingdomName = kingdom != null ? kingdom.getName() : "Unknown";
                plugin.getMessageManager().sendMessage(player, "territory.protected", kingdomName);
                event.setCancelled(true);
                return;
            }
        } else if (territory != null && !feudalPlayer.hasKingdom()) {
            // Non-kingdom players cannot build in claimed territory
            Kingdom kingdom = plugin.getKingdomManager().getKingdom(territory.getKingdomId());
            String kingdomName = kingdom != null ? kingdom.getName() : "Unknown";
            plugin.getMessageManager().sendMessage(player, "territory.protected", kingdomName);
            event.setCancelled(true);
            return;
        }
        
        // Award profession experience for mining
        if (isMiningBlock(block.getType())) {
            plugin.getPlayerDataManager().addExperience(player.getUniqueId(), Profession.MINER, 5);
        }
        
        // Award profession experience for farming
        if (isFarmingBlock(block.getType())) {
            plugin.getPlayerDataManager().addExperience(player.getUniqueId(), Profession.FARMER, 3);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Check territory permissions with alliance support
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        Territory territory = plugin.getKingdomManager().getTerritoryAt(block.getLocation());
        
        if (territory != null && feudalPlayer.hasKingdom()) {
            UUID playerKingdomId = feudalPlayer.getKingdom().getKingdomId();
            UUID territoryOwnerId = territory.getKingdomId();
            
            // Check if player can interact in this territory (own, ally, or nation member)
            if (!plugin.getAllianceManager().canInteractInTerritory(playerKingdomId, territoryOwnerId)) {
                Kingdom kingdom = plugin.getKingdomManager().getKingdom(territoryOwnerId);
                String kingdomName = kingdom != null ? kingdom.getName() : "Unknown";
                plugin.getMessageManager().sendMessage(player, "territory.protected", kingdomName);
                event.setCancelled(true);
                return;
            }
        } else if (territory != null && !feudalPlayer.hasKingdom()) {
            // Non-kingdom players cannot build in claimed territory
            Kingdom kingdom = plugin.getKingdomManager().getKingdom(territory.getKingdomId());
            String kingdomName = kingdom != null ? kingdom.getName() : "Unknown";
            plugin.getMessageManager().sendMessage(player, "territory.protected", kingdomName);
            event.setCancelled(true);
            return;
        }
        
        // Award profession experience for building
        if (isBuildingBlock(block.getType())) {
            plugin.getPlayerDataManager().addExperience(player.getUniqueId(), Profession.BUILDER, 2);
        }
        
        // Award profession experience for farming
        if (isFarmingBlock(block.getType())) {
            plugin.getPlayerDataManager().addExperience(player.getUniqueId(), Profession.FARMER, 2);
        }
    }
    
    private boolean isMiningBlock(Material material) {
        return switch (material) {
            case STONE, COBBLESTONE, DEEPSLATE, COBBLED_DEEPSLATE,
                 COAL_ORE, DEEPSLATE_COAL_ORE,
                 IRON_ORE, DEEPSLATE_IRON_ORE,
                 GOLD_ORE, DEEPSLATE_GOLD_ORE,
                 DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                 EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                 LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                 COPPER_ORE, DEEPSLATE_COPPER_ORE -> true;
            default -> false;
        };
    }
    
    private boolean isFarmingBlock(Material material) {
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS,
                 PUMPKIN, MELON, SUGAR_CANE,
                 COCOA, SWEET_BERRY_BUSH,
                 FARMLAND -> true;
            default -> false;
        };
    }
    
    private boolean isBuildingBlock(Material material) {
        return switch (material) {
            case STONE_BRICKS, COBBLESTONE, STONE,
                 OAK_PLANKS, SPRUCE_PLANKS, BIRCH_PLANKS, JUNGLE_PLANKS,
                 ACACIA_PLANKS, DARK_OAK_PLANKS, MANGROVE_PLANKS, CHERRY_PLANKS,
                 BRICKS, NETHER_BRICKS, END_STONE_BRICKS,
                 QUARTZ_BLOCK, SANDSTONE, RED_SANDSTONE,
                 WHITE_CONCRETE, ORANGE_CONCRETE, MAGENTA_CONCRETE,
                 LIGHT_BLUE_CONCRETE, YELLOW_CONCRETE, LIME_CONCRETE, PINK_CONCRETE,
                 GRAY_CONCRETE, LIGHT_GRAY_CONCRETE, CYAN_CONCRETE, PURPLE_CONCRETE,
                 BLUE_CONCRETE, BROWN_CONCRETE, GREEN_CONCRETE, RED_CONCRETE, BLACK_CONCRETE -> true;
            default -> false;
        };
    }
}
