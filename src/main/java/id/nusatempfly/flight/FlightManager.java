package id.nusatempfly.flight;

import id.nusatempfly.Plugin;
import id.nusatempfly.data.PlayerData;
import id.nusatempfly.hooks.WorldGuardHook;
import id.nusatempfly.util.TimeFormatter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlightManager {
    private final Plugin plugin;
    private BukkitTask flightTimeTask;
    private final Map<UUID, Boolean> previousFlightStates = new HashMap<>();
    private final TimeFormatter timeFormatter;
    private WorldGuardHook worldGuardHook;

    public FlightManager(Plugin plugin) {
        this.plugin = plugin;
        this.timeFormatter = new TimeFormatter(plugin);
    }
    
    /**
     * Set the WorldGuard hook
     * @param worldGuardHook The WorldGuard hook
     */
    public void setWorldGuardHook(WorldGuardHook worldGuardHook) {
        this.worldGuardHook = worldGuardHook;
    }

    /**
     * Enable flight for a player
     * @param player The player
     * @return true if flight was enabled, false if player had no time
     */
    public boolean enableFlight(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(uuid);
        
        // Check if player has time (or has bypass permission)
        if (!playerData.hasFlightTimeRemaining() && !player.hasPermission("nusatempfly.bypass.timelimit")) {
            return false;
        }
        
        // Check WorldGuard region if the integration is enabled
        if (worldGuardHook != null && worldGuardHook.isWorldGuardEnabled() && 
                !worldGuardHook.canFlyAtLocation(player) && 
                !player.hasPermission("nusatempfly.bypass.region")) {
            String message = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("worldguard.no-fly-region-enter-message"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.prefix") + message));
            return false;
        }

        // Save previous flight state before enabling
        previousFlightStates.put(uuid, player.getAllowFlight());
        
        // Enable flight
        player.setAllowFlight(true);
        player.setFlying(true);
        
        // Update player data
        playerData.setFlightEnabled(true);
        
        return true;
    }

    /**
     * Disable flight for a player
     * @param player The player
     */
    public void disableFlight(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(uuid);
        
        // Disable flight
        player.setAllowFlight(false);
        player.setFlying(false);
        
        // Update player data
        playerData.setFlightEnabled(false);
    }

    /**
     * Toggle flight for a player
     * @param player The player
     * @return true if flight was enabled, false if disabled
     */
    public boolean toggleFlight(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(uuid);
        boolean isFlightEnabled = playerData.isFlightEnabled();
        
        if (isFlightEnabled) {
            disableFlight(player);
            return false;
        } else {
            return enableFlight(player);
        }
    }

    /**
     * Add flight time to player with booster consideration
     * @param player The player
     * @param seconds Base time in seconds to add
     * @return Total time added (with booster)
     */
    public long addFlightTime(Player player, long seconds) {
        UUID uuid = player.getUniqueId();
        double boosterMultiplier = getBoosterMultiplier(player);
        long boostedTime = (long) (seconds * boosterMultiplier);
        long boostedExtra = boostedTime - seconds; // Extra time from booster
        
        long totalTime = plugin.getPlayerDataManager().addFlightTime(uuid, boostedTime);
        
        // Send message
        if (boostedExtra > 0) {
            // Player has booster
            String originalTimeStr = timeFormatter.format(seconds);
            String boosterTimeStr = timeFormatter.format(boostedExtra);
            String message = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.time-added-booster")
                    .replace("%original_time%", originalTimeStr)
                    .replace("%booster_time%", boosterTimeStr));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.prefix") + message));
        } else {
            // Player has no booster
            String timeStr = timeFormatter.format(seconds);
            String message = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.time-added")
                    .replace("%time%", timeStr));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.prefix") + message));
        }
        
        return boostedTime;
    }

    /**
     * Remove flight time from player
     * @param player The player
     * @param seconds Time to remove
     * @return Remaining time after removal
     */
    public long removeFlightTime(Player player, long seconds) {
        UUID uuid = player.getUniqueId();
        long remainingTime = plugin.getPlayerDataManager().removeFlightTime(uuid, seconds);
        
        // Send message
        String timeStr = timeFormatter.format(seconds);
        String message = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("messages.time-removed")
                .replace("%time%", timeStr));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("messages.prefix") + message));
        
        // Check if player should have flight disabled
        if (remainingTime <= 0 && plugin.getPlayerDataManager().isFlightEnabled(uuid)) {
            if (plugin.getConfig().getBoolean("flight.disable-on-expiry")) {
                disableFlight(player);
                String expiredMessage = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.flight-expired"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix") + expiredMessage));
            }
        }
        
        return remainingTime;
    }

    /**
     * Start task to track flight time
     */
    public void startFlightTimeTask() {
        // Cancel existing task if any
        if (flightTimeTask != null) {
            flightTimeTask.cancel();
        }
        
        // Start new task (runs every second)
        flightTimeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(uuid);
                
                // Jika pemain sedang terbang tapi statusnya tidak tercatat, 
                // sinkronkan statusnya terlebih dahulu
                if (player.isFlying() && !playerData.isFlightEnabled()) {
                    synchronizeFlightState(player);
                }
                
                // Skip players who are not using flight
                if (!playerData.isFlightEnabled()) {
                    continue;
                }
                
                // Skip time reduction if player has bypass permission
                if (player.hasPermission("nusatempfly.bypass.timelimit")) {
                    continue;
                }
                
                // Check if we should pause time in no-fly regions
                boolean pauseTime = false;
                if (worldGuardHook != null && worldGuardHook.isWorldGuardEnabled() && 
                    plugin.getConfig().getBoolean("worldguard.pause-time-in-no-fly-regions", true)) {
                    
                    // Pause time if player is in a no-fly region
                    pauseTime = !worldGuardHook.canFlyAtLocation(player);
                }
                
                // Only reduce time if not paused
                if (!pauseTime) {
                    // Reduce flight time
                    boolean hasTimeRemaining = playerData.reduceFlightTime(1);
                    
                    // Check if time expired
                    if (!hasTimeRemaining && plugin.getConfig().getBoolean("flight.disable-on-expiry")) {
                        disableFlight(player);
                        String expiredMessage = ChatColor.translateAlternateColorCodes('&', 
                            plugin.getConfig().getString("messages.flight-expired"));
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            plugin.getConfig().getString("messages.prefix") + expiredMessage));
                    }
                }
            }
        }, 20L, 20L); // 20 ticks = 1 second
    }

    /**
     * Stop flight time task
     */
    public void stopFlightTimeTask() {
        if (flightTimeTask != null) {
            flightTimeTask.cancel();
            flightTimeTask = null;
        }
    }

    /**
     * Get booster multiplier for a player
     * @param player The player
     * @return Booster multiplier (1.0 for no boost)
     */
    public double getBoosterMultiplier(Player player) {
        if (player.hasPermission("nusatempfly.booster")) {
            return plugin.getConfig().getDouble("flight.booster-multiplier", 1.5);
        }
        return 1.0; // No boost
    }

    /**
     * Restore flight state for a player when they log in
     * @param player The player
     */
    public void restoreFlightState(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(uuid);
        
        // Jika status flight diaktifkan dalam data dan pemain masih memiliki waktu 
        // atau memiliki izin bypass, aktifkan terbang
        if (playerData.isFlightEnabled() && 
            (playerData.hasFlightTimeRemaining() || player.hasPermission("nusatempfly.bypass.timelimit"))) {
            
            // Aktifkan terbang
            player.setAllowFlight(true);
            player.setFlying(true);
            
            plugin.getLogger().info("Restored flight for " + player.getName());
        } else if (playerData.isFlightEnabled() && !playerData.hasFlightTimeRemaining() && 
                   !player.hasPermission("nusatempfly.bypass.timelimit")) {
            // Jika pemain kehabisan waktu tapi status masih aktif, nonaktifkan
            playerData.setFlightEnabled(false);
            player.setAllowFlight(false);
            player.setFlying(false);
            plugin.getLogger().info("Cleared invalid flight state for " + player.getName());
        }
    }

    /**
     * Save flight state for a player when they log out
     * @param player The player
     */
    public void saveFlightState(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(uuid);
        
        // Save whether the player was flying
        playerData.setFlightEnabled(player.isFlying());
        
        // If configured to do so, save player data immediately
        if (plugin.getConfig().getBoolean("flight.save-on-disconnect")) {
            plugin.getPlayerDataManager().savePlayerData(uuid);
        }
    }

    /**
     * Synchronize flight state for a player
     * This ensures the playerData.isFlightEnabled matches player's actual flight state
     * @param player The player
     */
    public void synchronizeFlightState(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(uuid);
        
        // Jika pemain benar-benar terbang (isFlying) tapi statusnya tidak diaktifkan di data
        if (player.isFlying() && !playerData.isFlightEnabled()) {
            // Periksa apakah pemain memiliki waktu terbang atau bypass
            if (playerData.hasFlightTimeRemaining() || player.hasPermission("nusatempfly.bypass.timelimit")) {
                playerData.setFlightEnabled(true);
                plugin.getLogger().info("Synchronized flight state for " + player.getName() + " (enabled)");
            } else {
                // Pemain tidak punya waktu terbang tapi masih terbang, nonaktifkan
                player.setAllowFlight(false);
                player.setFlying(false);
                plugin.getLogger().info("Disabled flight for " + player.getName() + " (no time)");
            }
        }
        // Jika pemain tidak terbang tapi status di data masih aktif
        else if (!player.isFlying() && playerData.isFlightEnabled()) {
            playerData.setFlightEnabled(false);
            plugin.getLogger().info("Synchronized flight state for " + player.getName() + " (disabled)");
        }
    }
}