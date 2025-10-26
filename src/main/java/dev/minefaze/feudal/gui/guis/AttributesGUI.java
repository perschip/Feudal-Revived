package dev.minefaze.feudal.gui.guis;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.gui.BaseGUI;
import dev.minefaze.feudal.gui.ItemBuilder;
import dev.minefaze.feudal.models.Attribute;
import dev.minefaze.feudal.models.FeudalPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AttributesGUI extends BaseGUI {
    
    public AttributesGUI(Feudal plugin, Player player) {
        super(plugin, player, "§d§lAttributes", 36);
    }
    
    @Override
    public void initializeItems() {
        // Fill background
        ItemStack filler = ItemBuilder.createFiller(Material.PURPLE_STAINED_GLASS_PANE);
        fillEmpty(filler);
        
        FeudalPlayer feudalPlayer = plugin.getPlayerDataManager().getOrCreatePlayer(player);
        int availablePoints = feudalPlayer.getAvailableAttributePoints();
        
        // Strength (slot 10)
        int strength = feudalPlayer.getAttribute(Attribute.STRENGTH);
        setItem(10, new ItemBuilder(Material.IRON_SWORD)
            .name("§c§lStrength")
            .lore(
                "§7Increases your damage output",
                "§7in combat situations.",
                "",
                "§7Current Value: §e" + strength,
                "§7Damage Bonus: §c+" + (strength * 0.5) + " hearts",
                "",
                "§7Effects:",
                "§8• §7Each point adds 0.5 hearts damage",
                "§8• §7Affects all weapon types",
                "§8• §7Stacks with profession bonuses",
                "",
                availablePoints > 0 ? "§a§lClick to upgrade! §7(Cost: 1 point)" : "§c§lNo points available",
                "§e§lRight-click for details!"
            )
            .hideAttributes()
            .build(),
            (p, click, item) -> {
                if (click.isRightClick()) {
                    plugin.getGUIManager().openAttributeDetails(p, "strength");
                } else if (click.isLeftClick() && availablePoints > 0) {
                    plugin.getPlayerDataManager().addAttributePoints(p.getUniqueId(), Attribute.STRENGTH, 1);
                    p.sendMessage("§a§lAttribute Upgraded! §7Strength increased by 1 point.");
                    refresh();
                }
            });
        
        // Defense (slot 12)
        int defense = feudalPlayer.getAttribute(Attribute.DEFENSE);
        setItem(12, new ItemBuilder(Material.IRON_CHESTPLATE)
            .name("§9§lDefense")
            .lore(
                "§7Reduces damage taken from",
                "§7all sources of harm.",
                "",
                "§7Current Value: §e" + defense,
                "§7Damage Reduction: §9-" + (defense * 0.3) + " hearts",
                "",
                "§7Effects:",
                "§8• §7Each point reduces 0.3 hearts damage",
                "§8• §7Affects all damage types",
                "§8• §7Stacks with armor protection",
                "",
                availablePoints > 0 ? "§a§lClick to upgrade! §7(Cost: 1 point)" : "§c§lNo points available",
                "§e§lRight-click for details!"
            )
            .hideAttributes()
            .build(),
            (p, click, item) -> {
                if (click.isRightClick()) {
                    plugin.getGUIManager().openAttributeDetails(p, "defense");
                } else if (click.isLeftClick() && availablePoints > 0) {
                    plugin.getPlayerDataManager().addAttributePoints(p.getUniqueId(), Attribute.DEFENSE, 1);
                    p.sendMessage("§a§lAttribute Upgraded! §7Defense increased by 1 point.");
                    refresh();
                }
            });
        
        // Agility (slot 14)
        int agility = feudalPlayer.getAttribute(Attribute.AGILITY);
        setItem(14, new ItemBuilder(Material.LEATHER_BOOTS)
            .name("§a§lAgility")
            .lore(
                "§7Increases your movement speed",
                "§7and combat mobility.",
                "",
                "§7Current Value: §e" + agility,
                "§7Speed Bonus: §a+" + (agility > 15 ? "Speed " + Math.min((agility - 15) / 5 + 1, 3) : "None"),
                "",
                "§7Effects:",
                "§8• §7Speed I at 15+ points",
                "§8• §7Speed II at 20+ points",
                "§8• §7Speed III at 25+ points",
                "",
                availablePoints > 0 ? "§a§lClick to upgrade! §7(Cost: 1 point)" : "§c§lNo points available",
                "§e§lRight-click for details!"
            )
            .hideAttributes()
            .build(),
            (p, click, item) -> {
                if (click.isRightClick()) {
                    plugin.getGUIManager().openAttributeDetails(p, "agility");
                } else if (click.isLeftClick() && availablePoints > 0) {
                    plugin.getPlayerDataManager().addAttributePoints(p.getUniqueId(), Attribute.AGILITY, 1);
                    p.sendMessage("§a§lAttribute Upgraded! §7Agility increased by 1 point.");
                    refresh();
                }
            });
        
        // Endurance (slot 16)
        int endurance = feudalPlayer.getAttribute(Attribute.ENDURANCE);
        setItem(16, new ItemBuilder(Material.GOLDEN_APPLE)
            .name("§6§lEndurance")
            .lore(
                "§7Increases your maximum health",
                "§7and survivability.",
                "",
                "§7Current Value: §e" + endurance,
                "§7Health Bonus: §6+" + (endurance > 15 ? Math.min((endurance - 15) / 3 + 1, 6) + " hearts" : "None"),
                "",
                "§7Effects:",
                "§8• §7+1 heart at 15+ points",
                "§8• §7+2 hearts at 18+ points",
                "§8• §7Up to +6 hearts maximum",
                "",
                availablePoints > 0 ? "§a§lClick to upgrade! §7(Cost: 1 point)" : "§c§lNo points available",
                "§e§lRight-click for details!"
            )
            .hideAttributes()
            .build(),
            (p, click, item) -> {
                if (click.isRightClick()) {
                    plugin.getGUIManager().openAttributeDetails(p, "endurance");
                } else if (click.isLeftClick() && availablePoints > 0) {
                    plugin.getPlayerDataManager().addAttributePoints(p.getUniqueId(), Attribute.ENDURANCE, 1);
                    p.sendMessage("§a§lAttribute Upgraded! §7Endurance increased by 1 point.");
                    refresh();
                }
            });
        
        // Attribute overview (slot 22)
        int totalAttributes = strength + defense + agility + endurance;
        setItem(22, new ItemBuilder(Material.NETHER_STAR)
            .name("§d§lAttribute Overview")
            .lore(
                "§7Your character's core attributes",
                "",
                "§7Available Points: §e" + availablePoints,
                "§7Total Allocated: §e" + totalAttributes,
                "§7Combat Power: §c" + feudalPlayer.getCombatPower(),
                "",
                "§7Current Attributes:",
                "§8• §cStrength: §e" + strength,
                "§8• §9Defense: §e" + defense,
                "§8• §aAgility: §e" + agility,
                "§8• §6Endurance: §e" + endurance,
                "",
                "§7Earn points by:",
                "§8• §7Leveling professions",
                "§8• §7Winning challenges",
                "§8• §7Completing achievements"
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
