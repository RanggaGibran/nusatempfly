package id.nusatempfly.placeholders;

import id.nusatempfly.Plugin;
import id.nusatempfly.util.TimeFormatter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI integration for NusaTempFly
 */
public class TempFlyPlaceholders extends PlaceholderExpansion {
    private final Plugin plugin;
    private final TimeFormatter timeFormatter;

    public TempFlyPlaceholders(Plugin plugin) {
        this.plugin = plugin;
        this.timeFormatter = new TimeFormatter(plugin);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "tempfly";
    }

    @Override
    public @NotNull String getAuthor() {
        return "NusaDev";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // This is required or placeholders will stop working on reload
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // Get online player if available (some placeholders require an online player)
        Player onlinePlayer = player.getPlayer();
        
        // Handle different placeholder requests
        switch (params.toLowerCase()) {
            // Time remaining placeholders
            case "time_remaining":
                return String.valueOf(plugin.getPlayerDataManager().getRemainingFlightTime(player.getUniqueId()));
            case "time_formatted":
                return timeFormatter.format(plugin.getPlayerDataManager().getRemainingFlightTime(player.getUniqueId()));
            case "time_compact":
                return timeFormatter.formatCompact(plugin.getPlayerDataManager().getRemainingFlightTime(player.getUniqueId()));
                
            // Flight status placeholders
            case "enabled":
                return plugin.getPlayerDataManager().isFlightEnabled(player.getUniqueId()) ? "Yes" : "No";
            case "enabled_bool":
                return plugin.getPlayerDataManager().isFlightEnabled(player.getUniqueId()) ? "true" : "false";
                
            // Booster placeholders
            case "has_booster":
                if (onlinePlayer == null) return "false";
                return onlinePlayer.hasPermission("nusatempfly.booster") ? "true" : "false";
            case "booster_multiplier":
                if (onlinePlayer == null) return "1.0";
                return String.valueOf(onlinePlayer.hasPermission("nusatempfly.booster") ? 
                        plugin.getConfig().getDouble("flight.booster-multiplier", 1.5) : 1.0);
            case "booster_percentage":
                if (onlinePlayer == null) return "0%";
                if (!onlinePlayer.hasPermission("nusatempfly.booster")) return "0%";
                double multiplier = plugin.getConfig().getDouble("flight.booster-multiplier", 1.5);
                int percentage = (int) ((multiplier - 1.0) * 100);
                return percentage + "%";
                
            // Time components placeholders
            case "time_days":
                long seconds = plugin.getPlayerDataManager().getRemainingFlightTime(player.getUniqueId());
                return String.valueOf(seconds / 86400);
            case "time_hours":
                seconds = plugin.getPlayerDataManager().getRemainingFlightTime(player.getUniqueId());
                return String.valueOf((seconds % 86400) / 3600);
            case "time_minutes":
                seconds = plugin.getPlayerDataManager().getRemainingFlightTime(player.getUniqueId());
                return String.valueOf((seconds % 3600) / 60);
            case "time_seconds":
                seconds = plugin.getPlayerDataManager().getRemainingFlightTime(player.getUniqueId());
                return String.valueOf(seconds % 60);
                
            // Invalid placeholder
            default:
                return null;
        }
    }
}