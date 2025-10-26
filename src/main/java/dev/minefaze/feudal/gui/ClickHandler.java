package dev.minefaze.feudal.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

@FunctionalInterface
public interface ClickHandler {
    
    /**
     * Handle a click event in the GUI
     * @param player The player who clicked
     * @param clickType The type of click (LEFT, RIGHT, SHIFT_LEFT, etc.)
     * @param item The item that was clicked (can be null)
     */
    void onClick(Player player, ClickType clickType, ItemStack item);
}
