package dev.minefaze.feudal.gui.guis;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.gui.BaseGUI;
import dev.minefaze.feudal.gui.ItemBuilder;
import dev.minefaze.feudal.models.FeudalPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MainMenuGUI extends BaseGUI {
    
    public MainMenuGUI(Feudal plugin, Player player) {
        super(plugin, player, "§6§lFEUDAL RPG", 45);
    }
    
    @Override
    public void initializeItems() {
        // Fill background
        ItemStack filler = ItemBuilder.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        fillEmpty(filler);
        
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        
        // Player stats display (top row)
        setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
            .name("§e§l" + player.getName())
            .lore(
                "§7Total Experience: §e" + feudalPlayer.getTotalExperience(),
                "§7Combat Power: §c" + feudalPlayer.getCombatPower(),
                "§7Kingdom: " + (feudalPlayer.hasKingdom() ? "§a" + feudalPlayer.getKingdom().getName() : "§cNone"),
                "",
                "§7Your character overview"
            )
            .build());
        
        // Main menu options
        
        // Professions (slot 19)
        setItem(19, new ItemBuilder(Material.DIAMOND_PICKAXE)
            .name("§b§lProfessions")
            .lore(
                "§7Level up your skills and unlock",
                "§7new abilities and bonuses.",
                "",
                "§7Available Professions:",
                "§8• §7Warrior §8- §eCombat specialist",
                "§8• §7Miner §8- §eResource gathering",
                "§8• §7Builder §8- §eConstruction expert",
                "§8• §7Farmer §8- §eFood production",
                "",
                "§e§lClick to view professions!"
            )
            .hideAttributes()
            .build(), 
            (p, click, item) -> plugin.getGUIManager().openProfessionsMenu(p));
        
        // Attributes (slot 21)
        setItem(21, new ItemBuilder(Material.ENCHANTED_BOOK)
            .name("§d§lAttributes")
            .lore(
                "§7Enhance your character's core",
                "§7abilities and combat effectiveness.",
                "",
                "§7Available Attributes:",
                "§8• §7Strength §8- §eIncreases damage",
                "§8• §7Defense §8- §eReduces damage taken",
                "§8• §7Agility §8- §eIncreases speed",
                "§8• §7Endurance §8- §eIncreases health",
                "",
                "§e§lClick to manage attributes!"
            )
            .glow()
            .build(),
            (p, click, item) -> plugin.getGUIManager().openAttributesMenu(p));
        
        // Kingdoms (slot 23)
        setItem(23, new ItemBuilder(Material.GOLDEN_SWORD)
            .name("§6§lKingdoms")
            .lore(
                "§7Create or join a kingdom to",
                "§7conquer territories and build an empire.",
                "",
                "§7Kingdom Features:",
                "§8• §7Create your own kingdom",
                "§8• §7Claim and defend territories",
                "§8• §7Build with kingdom members",
                "§8• §7Wage wars against enemies",
                "",
                "§e§lClick to manage kingdoms!"
            )
            .hideAttributes()
            .build(),
            (p, click, item) -> plugin.getGUIManager().openKingdomMenu(p));
        
        // Challenges (slot 25)
        setItem(25, new ItemBuilder(Material.IRON_SWORD)
            .name("§c§lChallenges")
            .lore(
                "§7Challenge other players to",
                "§7honorable combat and conquest.",
                "",
                "§7Challenge Types:",
                "§8• §7Honor Duel §8- §e1v1 combat",
                "§8• §7Land Conquest §8- §eTerritory battles",
                "§8• §7Resource Raid §8- §eWealth battles",
                "",
                "§e§lClick to view challenges!"
            )
            .hideAttributes()
            .build(),
            (p, click, item) -> plugin.getGUIManager().openChallengeMenu(p));
        
        // Market (slot 31)
        setItem(31, new ItemBuilder(Material.EMERALD)
            .name("§a§lMarket")
            .lore(
                "§7Trade resources, equipment,",
                "§7and rare items with other players.",
                "",
                "§7Market Features:",
                "§8• §7Buy and sell items",
                "§8• §7Browse player shops",
                "§8• §7Auction rare equipment",
                "§8• §7Kingdom trade agreements",
                "",
                "§e§lClick to open market!"
            )
            .glow()
            .build(),
            (p, click, item) -> plugin.getGUIManager().openMarketMenu(p));
        
        // Close button
        setItem(40, ItemBuilder.createCloseButton(), 
            (p, click, item) -> p.closeInventory());
    }
    
    @Override
    public void refresh() {
        inventory.clear();
        initializeItems();
    }
}
