package dev.minefaze.feudal.gui.guis;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.gui.BaseGUI;
import dev.minefaze.feudal.gui.ItemBuilder;
import dev.minefaze.feudal.models.FeudalPlayer;
import dev.minefaze.feudal.models.Profession;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ProfessionsGUI extends BaseGUI {
    
    public ProfessionsGUI(Feudal plugin, Player player) {
        super(plugin, player, "§b§lProfessions", 36);
    }
    
    @Override
    public void initializeItems() {
        // Fill background
        ItemStack filler = ItemBuilder.createFiller(Material.BLUE_STAINED_GLASS_PANE);
        fillEmpty(filler);
        
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        
        // Warrior (slot 10)
        int warriorLevel = feudalPlayer.getProfessionLevel(Profession.WARRIOR);
        setItem(10, new ItemBuilder(Material.DIAMOND_SWORD)
            .name("§c§lWarrior")
            .lore(
                "§7Master of combat and warfare",
                "",
                "§7Current Level: §e" + warriorLevel,
                "§7Experience: §e" + feudalPlayer.getProfessionExperience(Profession.WARRIOR),
                "",
                "§7Bonuses:",
                "§8• §7Increased damage in combat",
                "§8• §7Resistance at higher levels",
                "§8• §7Access to warrior equipment",
                "",
                "§7Level " + (warriorLevel + 1) + " Benefits:",
                "§8• §7+" + (warriorLevel + 1) + " Combat Power",
                warriorLevel >= 5 ? "§8• §7Damage Resistance I" : "§8• §7Damage Resistance I §c(Level 5+)",
                "",
                "§e§lClick for details!"
            )
            .hideAttributes()
            .build(),
            (p, click, item) -> plugin.getGUIManager().openProfessionDetails(p, "warrior"));
        
        // Miner (slot 12)
        int minerLevel = feudalPlayer.getProfessionLevel(Profession.MINER);
        setItem(12, new ItemBuilder(Material.DIAMOND_PICKAXE)
            .name("§8§lMiner")
            .lore(
                "§7Expert in resource extraction",
                "",
                "§7Current Level: §e" + minerLevel,
                "§7Experience: §e" + feudalPlayer.getProfessionExperience(Profession.MINER),
                "",
                "§7Bonuses:",
                "§8• §7Faster mining speed",
                "§8• §7Chance for bonus ores",
                "§8• §7Access to rare materials",
                "",
                "§7Level " + (minerLevel + 1) + " Benefits:",
                "§8• §7+" + ((minerLevel + 1) * 5) + "% Mining Speed",
                minerLevel >= 10 ? "§8• §7Fortune II on tools" : "§8• §7Fortune II on tools §c(Level 10+)",
                "",
                "§e§lClick for details!"
            )
            .hideAttributes()
            .build(),
            (p, click, item) -> plugin.getGUIManager().openProfessionDetails(p, "miner"));
        
        // Builder (slot 14)
        int builderLevel = feudalPlayer.getProfessionLevel(Profession.BUILDER);
        setItem(14, new ItemBuilder(Material.GOLDEN_AXE)
            .name("§6§lBuilder")
            .lore(
                "§7Master of construction and architecture",
                "",
                "§7Current Level: §e" + builderLevel,
                "§7Experience: §e" + feudalPlayer.getProfessionExperience(Profession.BUILDER),
                "",
                "§7Bonuses:",
                "§8• §7Faster building speed",
                "§8• §7Reduced material costs",
                "§8• §7Access to special blocks",
                "",
                "§7Level " + (builderLevel + 1) + " Benefits:",
                "§8• §7+" + ((builderLevel + 1) * 3) + "% Build Speed",
                builderLevel >= 15 ? "§8• §7Efficiency III on tools" : "§8• §7Efficiency III on tools §c(Level 15+)",
                "",
                "§e§lClick for details!"
            )
            .hideAttributes()
            .build(),
            (p, click, item) -> plugin.getGUIManager().openProfessionDetails(p, "builder"));
        
        // Farmer (slot 16)
        int farmerLevel = feudalPlayer.getProfessionLevel(Profession.FARMER);
        setItem(16, new ItemBuilder(Material.GOLDEN_HOE)
            .name("§a§lFarmer")
            .lore(
                "§7Expert in agriculture and food production",
                "",
                "§7Current Level: §e" + farmerLevel,
                "§7Experience: §e" + feudalPlayer.getProfessionExperience(Profession.FARMER),
                "",
                "§7Bonuses:",
                "§8• §7Faster crop growth",
                "§8• §7Higher crop yields",
                "§8• §7Access to rare seeds",
                "",
                "§7Level " + (farmerLevel + 1) + " Benefits:",
                "§8• §7+" + ((farmerLevel + 1) * 2) + "% Crop Yield",
                farmerLevel >= 8 ? "§8• §7Bone Meal efficiency" : "§8• §7Bone Meal efficiency §c(Level 8+)",
                "",
                "§e§lClick for details!"
            )
            .hideAttributes()
            .build(),
            (p, click, item) -> plugin.getGUIManager().openProfessionDetails(p, "farmer"));
        
        // Total profession stats (slot 22)
        int totalLevel = feudalPlayer.getTotalProfessionLevel();
        setItem(22, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
            .name("§e§lProfession Overview")
            .lore(
                "§7Your profession statistics",
                "",
                "§7Total Level: §e" + totalLevel,
                "§7Total Experience: §e" + feudalPlayer.getTotalExperience(),
                "§7Combat Power: §c" + feudalPlayer.getCombatPower(),
                "",
                "§7Profession Levels:",
                "§8• §cWarrior: §e" + warriorLevel,
                "§8• §8Miner: §e" + minerLevel,
                "§8• §6Builder: §e" + builderLevel,
                "§8• §aFarmer: §e" + farmerLevel
            )
            .glow()
            .build());
        
        // Back button
        setItem(31, ItemBuilder.createBackButton(),
            (p, click, item) -> plugin.getGUIManager().openMainMenu(p));
        
        // Close button
        setItem(32, ItemBuilder.createCloseButton(),
            (p, click, item) -> p.closeInventory());
    }
    
    @Override
    public void refresh() {
        inventory.clear();
        initializeItems();
    }
}
