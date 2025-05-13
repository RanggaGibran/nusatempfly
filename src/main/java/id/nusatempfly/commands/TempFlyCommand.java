package id.nusatempfly.commands;

import id.nusatempfly.Plugin;
import id.nusatempfly.util.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class TempFlyCommand implements CommandExecutor, TabCompleter {
    private final Plugin plugin;
    private final TimeFormatter timeFormatter;
    
    // List of admin subcommands
    private final List<String> adminCommands = Arrays.asList("give", "take", "set", "check", "reload");
    
    // List of player subcommands
    private final List<String> playerCommands = Arrays.asList("toggle", "check", "time");
    
    public TempFlyCommand(Plugin plugin) {
        this.plugin = plugin;
        this.timeFormatter = new TimeFormatter(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Toggle flight if no arguments (instead of showing help)
        if (args.length == 0) {
            // Only players can toggle flight
            if (sender instanceof Player) {
                handleToggleCommand(sender, args);
            } else {
                // Console users get the help menu instead
                sendHelp(sender);
            }
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        // Handle admin commands
        if (adminCommands.contains(subCommand) && !subCommand.equals("check")) {
            if (!sender.hasPermission("nusatempfly.admin")) {
                sendNoPermissionMessage(sender);
                return true;
            }
            
            switch (subCommand) {
                case "give":
                    handleGiveCommand(sender, args);
                    break;
                case "take":
                    handleTakeCommand(sender, args);
                    break;
                case "set":
                    handleSetCommand(sender, args);
                    break;
                case "reload":
                    handleReloadCommand(sender);
                    break;
            }
            return true;
        }
        
        // Handle player commands
        switch (subCommand) {
            case "toggle":
                handleToggleCommand(sender, args);
                break;
            case "check":
                handleCheckCommand(sender, args);
                break;
            case "time":
                handleTimeCommand(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    // Tab completion for command arguments
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - show subcommands based on permissions
            if (sender.hasPermission("nusatempfly.admin")) {
                for (String adminCmd : adminCommands) {
                    if (adminCmd.startsWith(args[0].toLowerCase())) {
                        completions.add(adminCmd);
                    }
                }
            }
            
            for (String playerCmd : playerCommands) {
                if (playerCmd.startsWith(args[0].toLowerCase())) {
                    completions.add(playerCmd);
                }
            }
        } else if (args.length == 2) {
            // Second argument - show players for relevant commands
            String subCommand = args[0].toLowerCase();
            if (Arrays.asList("give", "take", "set", "check").contains(subCommand) && 
                    sender.hasPermission("nusatempfly.admin")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        } else if (args.length == 3) {
            // Third argument - suggest time formats for relevant commands
            String subCommand = args[0].toLowerCase();
            if (Arrays.asList("give", "take", "set").contains(subCommand) && 
                    sender.hasPermission("nusatempfly.admin")) {
                if ("".startsWith(args[2].toLowerCase())) {
                    completions.add("1m");
                    completions.add("5m");
                    completions.add("10m");
                    completions.add("30m");
                    completions.add("1h");
                    completions.add("1d");
                }
            }
        }
        
        return completions;
    }
    
    // Display help message
    private void sendHelp(CommandSender sender) {
        String prefix = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.prefix"));
        
        sender.sendMessage(prefix + ChatColor.YELLOW + "NusaTempFly Commands:");
        sender.sendMessage(ChatColor.GOLD + "/" + "tempfly toggle" + ChatColor.WHITE + " - Toggle flight mode");
        sender.sendMessage(ChatColor.GOLD + "/" + "tempfly check" + ChatColor.WHITE + " - Check your flight time");
        
        if (sender.hasPermission("nusatempfly.admin")) {
            sender.sendMessage(ChatColor.RED + "Admin Commands:");
            sender.sendMessage(ChatColor.GOLD + "/" + "tempfly give <player> <time>" + ChatColor.WHITE + " - Give flight time");
            sender.sendMessage(ChatColor.GOLD + "/" + "tempfly take <player> <time>" + ChatColor.WHITE + " - Take flight time");
            sender.sendMessage(ChatColor.GOLD + "/" + "tempfly set <player> <time>" + ChatColor.WHITE + " - Set flight time");
            sender.sendMessage(ChatColor.GOLD + "/" + "tempfly check <player>" + ChatColor.WHITE + " - Check player's flight time");
            sender.sendMessage(ChatColor.GOLD + "/" + "tempfly reload" + ChatColor.WHITE + " - Reload plugin configuration");
        }
    }
    
    // Send no permission message
    private void sendNoPermissionMessage(CommandSender sender) {
        String message = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.prefix") + 
                plugin.getConfig().getString("messages.no-permission"));
        sender.sendMessage(message);
    }
    
    // Handle give command: /tempfly give <player> <time>
    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /tempfly give <player> <time>");
            return;
        }
        
        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            String message = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.player-not-found"));
            sender.sendMessage(message);
            return;
        }
        
        String timeArg = args[2];
        long seconds = timeFormatter.parse(timeArg);
        
        if (seconds <= 0) {
            // If invalid format or negative time, use default time
            seconds = plugin.getConfig().getLong("flight.default-time", 300);
        }
        
        // Add flight time using flight manager
        plugin.getFlightManager().addFlightTime(target, seconds);
        
        // Inform the command sender if different from target
        if (!sender.equals(target)) {
            String time = timeFormatter.format(seconds);
            sender.sendMessage(ChatColor.GREEN + "Gave " + target.getName() + " " + time + " of flight time.");
        }
    }
    
    // Handle take command: /tempfly take <player> <time>
    private void handleTakeCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /tempfly take <player> <time>");
            return;
        }
        
        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            String message = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.player-not-found"));
            sender.sendMessage(message);
            return;
        }
        
        String timeArg = args[2];
        long seconds = timeFormatter.parse(timeArg);
        
        if (seconds <= 0) {
            // If invalid format or negative time, inform user
            String message = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.invalid-time-format"));
            sender.sendMessage(message);
            return;
        }
        
        // Remove flight time using flight manager
        long remaining = plugin.getFlightManager().removeFlightTime(target, seconds);
        
        // Inform the command sender if different from target
        if (!sender.equals(target)) {
            String time = timeFormatter.format(seconds);
            String remainingTime = timeFormatter.format(remaining);
            sender.sendMessage(ChatColor.GREEN + "Took " + time + " of flight time from " + target.getName() + 
                    ". They now have " + remainingTime + " remaining.");
        }
    }
    
    // Handle set command: /tempfly set <player> <time>
    private void handleSetCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /tempfly set <player> <time>");
            return;
        }
        
        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            String message = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.player-not-found"));
            sender.sendMessage(message);
            return;
        }
        
        String timeArg = args[2];
        long seconds = timeFormatter.parse(timeArg);
        
        if (seconds < 0) {
            // If invalid format, inform user
            String message = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.invalid-time-format"));
            sender.sendMessage(message);
            return;
        }
        
        // Set flight time
        plugin.getPlayerDataManager().setFlightTime(target.getUniqueId(), seconds);
        
        // Send message to target
        String time = timeFormatter.format(seconds);
        String message = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.prefix") + 
                plugin.getConfig().getString("messages.time-set")
                    .replace("%time%", time));
        target.sendMessage(message);
        
        // Inform the command sender if different from target
        if (!sender.equals(target)) {
            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s flight time to " + time + ".");
        }
    }
    
    // Handle reload command: /tempfly reload
    private void handleReloadCommand(CommandSender sender) {
        // Reload config
        plugin.reloadConfig();
        
        // Inform sender
        sender.sendMessage(ChatColor.GREEN + "NusaTempFly configuration reloaded successfully!");
    }
    
    // Handle toggle command: /tempfly toggle
    private void handleToggleCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("nusatempfly.use")) {
            sendNoPermissionMessage(player);
            return;
        }
        
        // Check if player has flight time or bypass permission
        if (!plugin.getPlayerDataManager().getPlayerData(player).hasFlightTimeRemaining() && 
                !player.hasPermission("nusatempfly.bypass.timelimit")) {
            String prefix = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix"));
            player.sendMessage(prefix + ChatColor.RED + "You don't have any flight time remaining!");
            return;
        }
        
        // Toggle flight
        plugin.getFlightManager().toggleFlight(player);
    }
    
    // Handle check command: /tempfly check [player]
    private void handleCheckCommand(CommandSender sender, String[] args) {
        if (args.length > 1) {
            // Check other player's time
            if (!sender.hasPermission("nusatempfly.admin")) {
                sendNoPermissionMessage(sender);
                return;
            }
            
            String playerName = args[1];
            Player target = Bukkit.getPlayer(playerName);
            
            if (target == null) {
                String message = ChatColor.translateAlternateColorCodes('&', 
                        plugin.getConfig().getString("messages.prefix") + 
                        plugin.getConfig().getString("messages.player-not-found"));
                sender.sendMessage(message);
                return;
            }
            
            // Get flight time
            long remainingTime = plugin.getPlayerDataManager().getRemainingFlightTime(target.getUniqueId());
            boolean isFlightEnabled = plugin.getPlayerDataManager().isFlightEnabled(target.getUniqueId());
            
            // Send info
            String prefix = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix"));
            sender.sendMessage(prefix + ChatColor.YELLOW + target.getName() + "'s Flight Info:");
            
            // Check if player has unlimited flight
            if (target.hasPermission("nusatempfly.bypass.timelimit")) {
                sender.sendMessage(ChatColor.YELLOW + "Remaining Time: " + 
                        ChatColor.GOLD + "∞ (Unlimited)");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Remaining Time: " + 
                        ChatColor.GREEN + timeFormatter.format(remainingTime));
            }
            
            sender.sendMessage(ChatColor.YELLOW + "Flight Enabled: " + 
                    (isFlightEnabled ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
            
            if (target.hasPermission("nusatempfly.booster")) {
                double multiplier = plugin.getConfig().getDouble("flight.booster-multiplier", 1.5);
                sender.sendMessage(ChatColor.YELLOW + "Booster: " + 
                        ChatColor.GREEN + "x" + multiplier);
            }
        } else {
            // Check own time
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Usage: /tempfly check <player>");
                return;
            }
            
            Player player = (Player) sender;
            
            // Get flight time
            long remainingTime = plugin.getPlayerDataManager().getRemainingFlightTime(player.getUniqueId());
            
            // Send message
            if (player.hasPermission("nusatempfly.bypass.timelimit")) {
                String message = ChatColor.translateAlternateColorCodes('&', 
                        plugin.getConfig().getString("messages.prefix") + 
                        "&aYou have &6∞ (Unlimited)&a flight time.");
                player.sendMessage(message);
            } else {
                String time = timeFormatter.format(remainingTime);
                String message = ChatColor.translateAlternateColorCodes('&', 
                        plugin.getConfig().getString("messages.prefix") + 
                        plugin.getConfig().getString("messages.time-check")
                            .replace("%time%", time));
                player.sendMessage(message);
            }
        }
    }
    
    // Handle time command: /tempfly time (alias for check)
    private void handleTimeCommand(CommandSender sender, String[] args) {
        // This is just an alias for "check" with no arguments
        handleCheckCommand(sender, new String[] { "check" });
    }
    
    /**
     * Send a non-intrusive notification to a player
     * @param player The player to notify
     * @param message The message to show
     */
    private void sendNotification(Player player, String message) {
        // Use action bar for notifications (less intrusive than chat)
        try {
            // Use spigot method if available (newer versions)
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
        } catch (Exception e) {
            // Fallback to title method with short duration if spigot method not available
            player.sendTitle("", message, 5, 40, 10);
        }
    }
}