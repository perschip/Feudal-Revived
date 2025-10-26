package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import dev.minefaze.feudal.managers.SchematicManager;
import dev.minefaze.feudal.models.Kingdom;
import dev.minefaze.feudal.models.TownHall;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command for managing schematics - admin only
 */
public class SchematicCommand implements SubCommand {
    
    private final Feudal plugin;
    
    public SchematicCommand(Feudal plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        if (!player.hasPermission("feudal.admin.schematic")) {
            plugin.getMessageManager().sendMessage(player, "general.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleReload(player);
            case "list":
                return handleList(player);
            case "build":
                return handleBuild(player, Arrays.copyOfRange(args, 1, args.length));
            case "info":
                return handleInfo(player);
            default:
                sendHelpMessage(player);
                return true;
        }
    }
    
    private boolean handleReload(Player player) {
        try {
            plugin.getSchematicManager().loadSchematics();
            player.sendMessage("§aSuccessfully reloaded all schematics!");
            return true;
        } catch (Exception e) {
            player.sendMessage("§cFailed to reload schematics: " + e.getMessage());
            plugin.getLogger().warning("Failed to reload schematics: " + e.getMessage());
            return true;
        }
    }
    
    private boolean handleList(Player player) {
        SchematicManager schematicManager = plugin.getSchematicManager();
        
        player.sendMessage("§6§l=== Loaded Schematics ===");
        
        if (schematicManager.getLoadedSchematicKeys().isEmpty()) {
            player.sendMessage("§7No schematics loaded.");
            return true;
        }
        
        for (String key : schematicManager.getLoadedSchematicKeys()) {
            player.sendMessage("§7- §e" + key);
        }
        
        player.sendMessage("§7Total: §e" + schematicManager.getLoadedSchematicKeys().size() + " §7schematics");
        return true;
    }
    
    private boolean handleBuild(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cUsage: /f schematic build <townhall|nexus> [type] [level]");
            return true;
        }
        
        String structureType = args[0].toLowerCase();
        
        switch (structureType) {
            case "townhall":
                return buildTownHall(player, args);
            case "nexus":
                return buildNexus(player, args);
            default:
                player.sendMessage("§cInvalid structure type. Use 'townhall' or 'nexus'");
                return true;
        }
    }
    
    private boolean buildTownHall(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /f schematic build townhall <medieval|fantasy|modern> <level>");
            return true;
        }
        
        String typeStr = args[1].toUpperCase();
        TownHall.TownHallType type;
        
        try {
            type = TownHall.TownHallType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid town hall type: " + args[1]);
            player.sendMessage("§7Available types: medieval, fantasy, modern");
            return true;
        }
        
        int level;
        try {
            level = Integer.parseInt(args[2]);
            if (level < 1 || level > 10) {
                player.sendMessage("§cLevel must be between 1 and 10");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid level: " + args[2]);
            return true;
        }
        
        // Create a temporary town hall for building (using null UUID for admin command)
        TownHall tempTownHall = new TownHall(null, player.getLocation(), type);
        tempTownHall.setLevel(level);
        
        plugin.getLogger().info("SchematicCommand: Building " + type.name() + " level " + level + " at " + player.getLocation());
        plugin.getSchematicManager().buildTownHall(tempTownHall, player.getLocation());
        player.sendMessage("§aBuilding " + type.getDisplayName() + " level " + level + " at your location!");
        
        return true;
    }
    
    private boolean buildNexus(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /f schematic build nexus <medieval|fantasy|modern>");
            return true;
        }
        
        String typeStr = args[1].toUpperCase();
        TownHall.TownHallType type;
        
        try {
            type = TownHall.TownHallType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid nexus type: " + args[1]);
            player.sendMessage("§7Available types: medieval, fantasy, modern");
            return true;
        }
        
        plugin.getSchematicManager().buildNexus(type, player.getLocation());
        player.sendMessage("§aBuilding " + type.getDisplayName() + " nexus at your location!");
        
        return true;
    }
    
    private boolean handleInfo(Player player) {
        SchematicManager schematicManager = plugin.getSchematicManager();
        
        player.sendMessage("§6§l=== Schematic Manager Info ===");
        player.sendMessage("§7Schematics Folder: §e" + schematicManager.getSchematicsFolder().getPath());
        player.sendMessage("§7Loaded Schematics: §e" + schematicManager.getLoadedSchematicKeys().size());
        player.sendMessage("§7Auto-Build Enabled: §e" + plugin.getConfig().getBoolean("townhall.auto-build", true));
        
        // Check for missing schematics
        List<String> missingTownHalls = new ArrayList<>();
        for (TownHall.TownHallType type : TownHall.TownHallType.values()) {
            for (int level = 1; level <= 10; level++) {
                String key = "townhall_" + type.name().toLowerCase() + "_" + level;
                if (!schematicManager.hasSchematic(key)) {
                    missingTownHalls.add(key);
                }
            }
        }
        
        if (!missingTownHalls.isEmpty()) {
            player.sendMessage("§c§lMissing Town Hall Schematics:");
            for (String missing : missingTownHalls) {
                player.sendMessage("§7- §c" + missing);
            }
        }
        
        List<String> missingNexus = new ArrayList<>();
        for (TownHall.TownHallType type : TownHall.TownHallType.values()) {
            String key = "nexus_" + type.name().toLowerCase();
            if (!schematicManager.hasSchematic(key)) {
                missingNexus.add(key);
            }
        }
        
        if (!missingNexus.isEmpty()) {
            player.sendMessage("§c§lMissing Nexus Schematics:");
            for (String missing : missingNexus) {
                player.sendMessage("§7- §c" + missing);
            }
        }
        
        return true;
    }
    
    private void sendHelpMessage(Player player) {
        player.sendMessage("§6§l=== Schematic Commands ===");
        player.sendMessage("§e/f schematic reload §7- Reload all schematics");
        player.sendMessage("§e/f schematic list §7- List loaded schematics");
        player.sendMessage("§e/f schematic info §7- Show schematic manager info");
        player.sendMessage("§e/f schematic build townhall <type> <level> §7- Build town hall");
        player.sendMessage("§e/f schematic build nexus <type> §7- Build nexus");
        player.sendMessage("§7Note: This command requires admin permissions");
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        if (!player.hasPermission("feudal.admin.schematic")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            return Arrays.asList("reload", "list", "build", "info");
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("build")) {
            return Arrays.asList("townhall", "nexus");
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("build")) {
            if (args[1].equalsIgnoreCase("townhall") || args[1].equalsIgnoreCase("nexus")) {
                return Arrays.asList("medieval", "fantasy", "modern");
            }
        }
        
        if (args.length == 4 && args[0].equalsIgnoreCase("build") && args[1].equalsIgnoreCase("townhall")) {
            return Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        }
        
        return new ArrayList<>();
    }
    
    @Override
    public String getName() {
        return "schematic";
    }
    
    @Override
    public String getDescription() {
        return "Manage and build schematics (Admin only)";
    }
    
    @Override
    public String getUsage() {
        return "/f schematic <reload|list|build|info>";
    }
    
    @Override
    public boolean hasPermission(Player player) {
        return player.hasPermission("feudal.admin.schematic");
    }
}
