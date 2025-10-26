package dev.minefaze.feudal.commands;

import dev.minefaze.feudal.Feudal;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HelpCommand implements SubCommand {
    
    private final Feudal plugin;
    private final CommandManager commandManager;
    private static final int COMMANDS_PER_PAGE = 5;
    
    public HelpCommand(Feudal plugin, CommandManager commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        // Get all commands the player has permission for
        List<SubCommand> availableCommands = commandManager.getSubCommands().stream()
                .filter(command -> command.hasPermission(player))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());
        
        // Parse page number
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid page number. Use: /feudal help [page]");
                return true;
            }
        }
        
        // Calculate pagination
        int totalPages = (int) Math.ceil((double) availableCommands.size() / COMMANDS_PER_PAGE);
        if (page < 1 || page > totalPages) {
            player.sendMessage("§cPage " + page + " doesn't exist. Available pages: 1-" + totalPages);
            return true;
        }
        
        int startIndex = (page - 1) * COMMANDS_PER_PAGE;
        int endIndex = Math.min(startIndex + COMMANDS_PER_PAGE, availableCommands.size());
        
        // Display header
        player.sendMessage("§6§l=== FEUDAL RPG COMMANDS (Page " + page + "/" + totalPages + ") ===");
        player.sendMessage("");
        
        // Display commands for this page
        for (int i = startIndex; i < endIndex; i++) {
            SubCommand command = availableCommands.get(i);
            String usage = command.getUsage().isEmpty() ? "" : " " + command.getUsage();
            player.sendMessage("§e/f " + command.getName() + usage + " §7- " + command.getDescription());
        }
        
        // Display clickable navigation
        player.sendMessage("");
        
        Component navigation = Component.empty();
        
        // Previous button
        if (page > 1) {
            Component previousButton = Component.text("[Previous]", NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/f help " + (page - 1)))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to go to page " + (page - 1), NamedTextColor.YELLOW)));
            navigation = navigation.append(previousButton);
        } else {
            Component previousButton = Component.text("[Previous]", NamedTextColor.DARK_GRAY);
            navigation = navigation.append(previousButton);
        }
        
        // Page info
        Component pageInfo = Component.text(" Page " + page + "/" + totalPages + " ", NamedTextColor.GRAY);
        navigation = navigation.append(pageInfo);
        
        // Next button
        if (page < totalPages) {
            Component nextButton = Component.text("[Next]", NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/f help " + (page + 1)))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to go to page " + (page + 1), NamedTextColor.YELLOW)));
            navigation = navigation.append(nextButton);
        } else {
            Component nextButton = Component.text("[Next]", NamedTextColor.DARK_GRAY);
            navigation = navigation.append(nextButton);
        }
        
        player.sendMessage(navigation);
        
        // Add instruction text
        if (page > 1 || page < totalPages) {
            player.sendMessage("§7Click the buttons above to navigate or use §e/f help [page]");
        }
        
        return true;
    }
    
    @Override
    public List<String> getTabCompletions(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Get all commands the player has permission for
            List<SubCommand> availableCommands = commandManager.getSubCommands().stream()
                    .filter(command -> command.hasPermission(player))
                    .collect(Collectors.toList());
            
            int totalPages = (int) Math.ceil((double) availableCommands.size() / COMMANDS_PER_PAGE);
            
            // Add page numbers as tab completions
            for (int i = 1; i <= totalPages; i++) {
                completions.add(String.valueOf(i));
            }
        }
        
        return completions;
    }
    
    @Override
    public String getName() {
        return "help";
    }
    
    @Override
    public String getDescription() {
        return "Show all available commands with pagination";
    }
    
    @Override
    public String getUsage() {
        return "[page]";
    }
}
