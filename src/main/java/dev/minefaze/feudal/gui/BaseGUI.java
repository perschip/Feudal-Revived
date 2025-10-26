package dev.minefaze.feudal.gui;

import dev.minefaze.feudal.Feudal;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class BaseGUI implements Listener {
    
    protected final Feudal plugin;
    protected final Player player;
    protected final String title;
    protected final int size;
    protected Inventory inventory;
    
    // Static map to track open GUIs
    private static final Map<UUID, BaseGUI> openGUIs = new HashMap<>();
    
    // Click handlers for specific slots
    protected final Map<Integer, ClickHandler> clickHandlers = new HashMap<>();
    
    public BaseGUI(Feudal plugin, Player player, String title, int size) {
        this.plugin = plugin;
        this.player = player;
        this.title = title;
        this.size = size;
        this.inventory = Bukkit.createInventory(null, size, title);
    }
    
    /**
     * Initialize the GUI content - must be implemented by subclasses
     */
    public abstract void initializeItems();
    
    /**
     * Handle GUI updates when data changes
     */
    public abstract void refresh();
    
    /**
     * Open the GUI for the player
     */
    public void open() {
        initializeItems();
        player.openInventory(inventory);
        openGUIs.put(player.getUniqueId(), this);
        
        // Register this GUI as a listener if not already registered
        if (!plugin.getServer().getPluginManager().isPluginEnabled(plugin)) return;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Close the GUI
     */
    public void close() {
        player.closeInventory();
        openGUIs.remove(player.getUniqueId());
    }
    
    /**
     * Set an item in a specific slot with a click handler
     */
    protected void setItem(int slot, ItemStack item, ClickHandler handler) {
        inventory.setItem(slot, item);
        if (handler != null) {
            clickHandlers.put(slot, handler);
        }
    }
    
    /**
     * Set an item in a specific slot without a click handler
     */
    protected void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }
    
    /**
     * Fill empty slots with a filler item
     */
    protected void fillEmpty(ItemStack filler) {
        for (int i = 0; i < size; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }
    
    /**
     * Get the GUI instance for a player
     */
    public static BaseGUI getOpenGUI(Player player) {
        return openGUIs.get(player.getUniqueId());
    }
    
    /**
     * Check if a player has a GUI open
     */
    public static boolean hasGUIOpen(Player player) {
        return openGUIs.containsKey(player.getUniqueId());
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clickedPlayer)) return;
        if (!clickedPlayer.equals(player)) return;
        if (!event.getInventory().equals(inventory)) return;
        
        event.setCancelled(true); // Cancel all clicks by default
        
        int slot = event.getSlot();
        ClickHandler handler = clickHandlers.get(slot);
        
        if (handler != null) {
            handler.onClick(clickedPlayer, event.getClick(), event.getCurrentItem());
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player closedPlayer)) return;
        if (!closedPlayer.equals(player)) return;
        if (!event.getInventory().equals(inventory)) return;
        
        openGUIs.remove(player.getUniqueId());
        onClose();
    }
    
    /**
     * Called when the GUI is closed - can be overridden by subclasses
     */
    protected void onClose() {
        // Default: do nothing
    }
    
    // Getters
    public Player getPlayer() { return player; }
    public String getTitle() { return title; }
    public int getSize() { return size; }
    public Inventory getInventory() { return inventory; }
}
