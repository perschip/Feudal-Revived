package dev.minefaze.feudal.gui;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemBuilder {
    
    private final ItemStack item;
    private final ItemMeta meta;
    
    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }
    
    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }
    
    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }
    
    public ItemBuilder name(String name) {
        if (meta != null) {
            meta.setDisplayName(name);
        }
        return this;
    }
    
    public ItemBuilder lore(String... lore) {
        if (meta != null) {
            meta.setLore(Arrays.asList(lore));
        }
        return this;
    }
    
    public ItemBuilder lore(List<String> lore) {
        if (meta != null) {
            meta.setLore(new ArrayList<>(lore));
        }
        return this;
    }
    
    public ItemBuilder addLore(String... lines) {
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.addAll(Arrays.asList(lines));
            meta.setLore(lore);
        }
        return this;
    }
    
    public ItemBuilder enchant(Enchantment enchantment, int level) {
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
        }
        return this;
    }
    
    public ItemBuilder glow() {
        if (meta != null) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }
    
    public ItemBuilder hideAttributes() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
        return this;
    }
    
    public ItemBuilder hideEnchants() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }
    
    public ItemBuilder hideAll() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
        }
        return this;
    }
    
    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }
    
    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
    
    // Static helper methods
    public static ItemStack createFiller(Material material) {
        return new ItemBuilder(material)
            .name("§r")
            .hideAll()
            .build();
    }
    
    public static ItemStack createBackButton() {
        return new ItemBuilder(Material.ARROW)
            .name("§c« Back")
            .lore("§7Click to go back")
            .build();
    }
    
    public static ItemStack createCloseButton() {
        return new ItemBuilder(Material.BARRIER)
            .name("§c✕ Close")
            .lore("§7Click to close this menu")
            .build();
    }
    
    public static ItemStack createNextPageButton() {
        return new ItemBuilder(Material.ARROW)
            .name("§a» Next Page")
            .lore("§7Click to go to the next page")
            .build();
    }
    
    public static ItemStack createPreviousPageButton() {
        return new ItemBuilder(Material.ARROW)
            .name("§c« Previous Page")
            .lore("§7Click to go to the previous page")
            .build();
    }
}
