package dev.minefaze.feudal.gui.guis;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.gui.BaseGUI;
import dev.minefaze.feudal.gui.ClickHandler;
import dev.minefaze.feudal.gui.ItemBuilder;
import dev.minefaze.feudal.models.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * Main Town Hall GUI for kingdom management
 */
public class TownHallGUI extends BaseGUI {
    
    private final Kingdom kingdom;
    
    public TownHallGUI(Feudal plugin, Player player, Kingdom kingdom) {
        super(plugin, player, "§6§l" + kingdom.getName() + " Town Hall", 54);
        this.kingdom = kingdom;
    }
    
    @Override
    public void initializeItems() {
        TownHall townHall = kingdom.getTownHall();
        Nexus nexus = kingdom.getNexus();
        
        if (townHall == null || nexus == null) {
            // Error state - shouldn't happen
            ItemStack error = new ItemBuilder(Material.BARRIER)
                .name("§c§lError")
                .lore(Arrays.asList("§7Town Hall or Nexus not found!"))
                .build();
            inventory.setItem(22, error);
            return;
        }
        
        // Town Hall Info
        ItemStack townHallInfo = createTownHallInfoItem(townHall);
        setItem(4, townHallInfo);
        
        // Nexus Info
        ItemStack nexusInfo = createNexusInfoItem(nexus);
        setItem(22, nexusInfo);
        
        // Upgrade Town Hall
        ItemStack upgrade = createUpgradeItem(townHall);
        
        // Kingdom Members
        ItemStack members = createMembersItem();
        
        // Kingdom Shop
        ItemStack shop = new ItemBuilder(Material.EMERALD)
            .name("§a§lKingdom Shop")
            .lore(Arrays.asList(
                "§7Buy and sell items with",
                "§7other kingdom members.",
                "",
                "§e§lClick to open!"
            ))
            .build();
        
        // Defenses
        ItemStack defenses = createDefensesItem(townHall);
        
        // Army Management
        ItemStack army = createArmyItem(townHall);
        
        // Kingdom Treasury
        ItemStack treasury = new ItemBuilder(Material.GOLD_INGOT)
            .name("§6§lKingdom Treasury")
            .lore(Arrays.asList(
                "§7Current Balance: §e" + kingdom.getTreasury() + " coins",
                "",
                "§7Manage kingdom finances",
                "§7and fund upgrades.",
                "",
                "§e§lClick to manage!"
            ))
            .build();
        
        // Kingdom Settings
        ItemStack settings = new ItemBuilder(Material.REDSTONE)
            .name("§c§lKingdom Settings")
            .lore(Arrays.asList(
                "§7Configure kingdom permissions,",
                "§7member roles, and other settings.",
                "",
                "§e§lClick to configure!"
            ))
            .build();
        
        // War Status
        ItemStack warStatus = new ItemBuilder(Material.DIAMOND_SWORD)
            .name("§4§lWar Status")
            .lore(Arrays.asList(
                "§7View active wars and",
                "§7declare new conflicts.",
                "",
                "§e§lClick to view!"
            ))
            .build();
        
        // Close button
        ItemStack close = new ItemBuilder(Material.BARRIER)
            .name("§c§lClose")
            .lore(Arrays.asList("§7Close the Town Hall"))
            .build();
        
        // Set click handlers
        setItem(10, upgrade, (player, clickType, item) -> handleUpgrade(player));
        setItem(12, members, (player, clickType, item) -> openMembersGUI(player));
        setItem(14, shop, (player, clickType, item) -> openShopGUI(player));
        setItem(16, defenses, (player, clickType, item) -> openDefensesGUI(player));
        setItem(28, army, (player, clickType, item) -> openArmyGUI(player));
        setItem(30, treasury, (player, clickType, item) -> openTreasuryGUI(player));
        setItem(32, settings, (player, clickType, item) -> openSettingsGUI(player));
        setItem(34, warStatus, (player, clickType, item) -> openWarGUI(player));
        setItem(49, close, (player, clickType, item) -> player.closeInventory());
    }
    
    private ItemStack createTownHallInfoItem(TownHall townHall) {
        return new ItemBuilder(townHall.getMainMaterial())
            .name("§6§lTown Hall Level " + townHall.getLevel())
            .lore(Arrays.asList(
                "§7Type: §e" + townHall.getType().getDisplayName(),
                "§7Level: §e" + townHall.getLevel() + "/10",
                "",
                "§e§lCapabilities:",
                "§7• Max Territories: §e" + townHall.getMaxTerritories(),
                "§7• Max Members: §e" + townHall.getMaxMembers(),
                "§7• Defense Slots: §e" + townHall.getDefenseSlots(),
                "§7• Army Camps: §e" + townHall.getArmyCamps(),
                "",
                townHall.isUpgrading() ? 
                    "§6§lUpgrading... §7(" + formatTime(townHall.getRemainingUpgradeTime()) + ")" :
                    "§a§lReady for action!"
            ))
            .build();
    }
    
    private ItemStack createNexusInfoItem(Nexus nexus) {
        return new ItemBuilder(Material.BEACON)
            .name("§b§lKingdom Nexus")
            .lore(Arrays.asList(
                "§7The heart of your kingdom",
                "",
                "§c❤ §7Health: " + nexus.getHealthColor() + nexus.getCurrentHealth() + "§7/" + nexus.getMaxHealth(),
                "§b🛡 §7Shield: §9" + nexus.getShieldPoints(),
                "§7Status: " + nexus.getHealthColor() + nexus.getStatus().getDisplayName(),
                "",
                nexus.getStatus().getHealthBar(),
                "",
                "§7The nexus represents your kingdom's",
                "§7strength in war. Protect it at all costs!",
                "",
                nexus.isRegenerating() ? "§a§lRegenerating..." : "§7Regeneration: §cInactive"
            ))
            .build();
    }
    
    private ItemStack createUpgradeItem(TownHall townHall) {
        if (!townHall.canUpgrade()) {
            return new ItemBuilder(Material.GRAY_DYE)
                .name("§7§lUpgrade Unavailable")
                .lore(Arrays.asList(
                    townHall.getLevel() >= 10 ? 
                        "§7Town Hall is at maximum level!" :
                        "§7Town Hall is currently upgrading..."
                ))
                .build();
        }
        
        return new ItemBuilder(Material.EXPERIENCE_BOTTLE)
            .name("§a§lUpgrade Town Hall")
            .lore(Arrays.asList(
                "§7Upgrade to Level " + (townHall.getLevel() + 1),
                "",
                "§e§lCost: §6" + townHall.getUpgradeCost() + " coins",
                "§e§lTime: §6" + formatTime(townHall.getUpgradeTime()),
                "",
                "§a§lBenefits:",
                "§7• More territories and members",
                "§7• Additional defense slots",
                "§7• Stronger nexus",
                "§7• New capabilities",
                "",
                "§e§lClick to upgrade!"
            ))
            .build();
    }
    
    private ItemStack createMembersItem() {
        return new ItemBuilder(Material.PLAYER_HEAD)
            .name("§9§lKingdom Members")
            .lore(Arrays.asList(
                "§7Members: §e" + kingdom.getMembers().size() + "§7/§e" + kingdom.getMaxMembers(),
                "",
                "§7Manage kingdom members,",
                "§7invite new players, and",
                "§7assign roles.",
                "",
                "§e§lClick to manage!"
            ))
            .build();
    }
    
    private ItemStack createDefensesItem(TownHall townHall) {
        return new ItemBuilder(Material.SHIELD)
            .name("§4§lDefenses")
            .lore(Arrays.asList(
                "§7Defense Slots: §e" + townHall.getDefenseSlots(),
                "",
                "§7Build and manage defensive",
                "§7structures to protect your kingdom.",
                "",
                "§e§lClick to manage!"
            ))
            .build();
    }
    
    private ItemStack createArmyItem(TownHall townHall) {
        return new ItemBuilder(Material.IRON_SWORD)
            .name("§c§lArmy Management")
            .lore(Arrays.asList(
                "§7Army Camps: §e" + townHall.getArmyCamps(),
                "",
                "§7Train and manage your",
                "§7kingdom's military forces.",
                "",
                "§e§lClick to manage!"
            ))
            .build();
    }
    
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }
    
    // Click handlers (placeholder implementations)
    private void handleUpgrade(Player player) {
        // TODO: Implement upgrade logic
        plugin.getMessageManager().sendMessage(player, "townhall.upgrade-coming-soon");
    }
    
    private void openMembersGUI(Player player) {
        // TODO: Implement members GUI
        plugin.getMessageManager().sendMessage(player, "townhall.feature-coming-soon");
    }
    
    private void openShopGUI(Player player) {
        // TODO: Implement shop GUI
        plugin.getMessageManager().sendMessage(player, "townhall.feature-coming-soon");
    }
    
    private void openDefensesGUI(Player player) {
        // TODO: Implement defenses GUI
        plugin.getMessageManager().sendMessage(player, "townhall.feature-coming-soon");
    }
    
    private void openArmyGUI(Player player) {
        // TODO: Implement army GUI
        plugin.getMessageManager().sendMessage(player, "townhall.feature-coming-soon");
    }
    
    private void openTreasuryGUI(Player player) {
        // TODO: Implement treasury GUI
        plugin.getMessageManager().sendMessage(player, "townhall.feature-coming-soon");
    }
    
    private void openSettingsGUI(Player player) {
        // TODO: Implement settings GUI
        plugin.getMessageManager().sendMessage(player, "townhall.feature-coming-soon");
    }
    
    private void openWarGUI(Player player) {
        // TODO: Implement war GUI
        plugin.getMessageManager().sendMessage(player, "townhall.feature-coming-soon");
    }
    
    @Override
    public void refresh() {
        // Clear inventory and reinitialize
        inventory.clear();
        initializeItems();
    }
}
