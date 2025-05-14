package id.nusatempfly.hooks;

import id.nusatempfly.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles WorldGuard integration for controlling flight in specific regions
 */
public class WorldGuardHook implements Listener {
    private final Plugin plugin;
    private boolean worldGuardEnabled = false;
    private StateFlag TEMP_FLY_ALLOWED;
    
    // Map to track players' last region status
    private final Map<UUID, Boolean> lastRegionAllowFlight = new HashMap<>();

    public WorldGuardHook(Plugin plugin) {
        this.plugin = plugin;
        
        // Check if WorldGuard is present and enabled in config
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null && 
                plugin.getConfig().getBoolean("worldguard.enabled", true)) {
            try {
                // Look for the flag first instead of trying to register it
                lookupFlag();
                worldGuardEnabled = true;
                plugin.getLogger().info("WorldGuard integration enabled successfully!");
                
                // Register events for tracking region entry/exit
                Bukkit.getPluginManager().registerEvents(this, plugin);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to initialize WorldGuard integration: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            plugin.getLogger().info("WorldGuard integration is disabled or WorldGuard is not installed.");
        }
    }
    
    /**
     * Lookup the flag from WorldGuard's registry instead of trying to register a new one
     */
    private void lookupFlag() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            // Try to look up our flag from the registry
            Flag<?> existingFlag = registry.get("tempfly-allowed");
            if (existingFlag instanceof StateFlag) {
                TEMP_FLY_ALLOWED = (StateFlag) existingFlag;
                plugin.getLogger().info("Found existing tempfly-allowed flag");
            } else {
                // If flag doesn't exist yet, create a default one
                // Note: This won't actually be registered with WorldGuard
                // but will work as a fallback value
                TEMP_FLY_ALLOWED = new StateFlag("tempfly-allowed", true);
                plugin.getLogger().info("Using default tempfly-allowed flag (not registered)");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to find WorldGuard flag: " + e.getMessage());
            // Create fallback flag
            TEMP_FLY_ALLOWED = new StateFlag("tempfly-allowed", true);
        }
    }
    
    /**
     * Check if a player can fly at their location
     * @param player The player
     * @return true if allowed, false if not
     */
    public boolean canFlyAtLocation(Player player) {
        if (!worldGuardEnabled || TEMP_FLY_ALLOWED == null) {
            return true; // If WorldGuard is not enabled, flight is allowed everywhere
        }
        
        // Check if player has bypass permission
        if (player.hasPermission("nusatempfly.bypass.region")) {
            return true;
        }
        
        try {
            // Get WorldGuard region container and query
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            
            // Check if flight is allowed at the player's location
            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(player.getLocation());
            
            // Test state flag directly for the location
            return query.testState(loc, null, TEMP_FLY_ALLOWED);
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking WorldGuard region state: " + e.getMessage());
            return true; // Default to allowing flight if there's an error
        }
    }
    
    /**
     * Track player movement between regions to toggle flight
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!worldGuardEnabled || TEMP_FLY_ALLOWED == null) {
            return;
        }
        
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Only check if the player has TempFly enabled
        if (!plugin.getPlayerDataManager().isFlightEnabled(uuid)) {
            return;
        }
        
        // Only check if the player has actually moved to a different block
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX() && 
            from.getBlockY() == to.getBlockY() && 
            from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        
        // Check if player can fly at the new location
        boolean canFly = canFlyAtLocation(player);
        
        // If this is the first time we're checking this player's region, initialize their status
        if (!lastRegionAllowFlight.containsKey(uuid)) {
            lastRegionAllowFlight.put(uuid, canFly);
            return;
        }
        
        // Check if the player's flight permission has changed
        boolean previousCanFly = lastRegionAllowFlight.get(uuid);
        
        if (previousCanFly && !canFly) {
            // Player entered a no-fly region
            handleEnterNoFlyRegion(player);
        } else if (!previousCanFly && canFly) {
            // Player exited a no-fly region
            handleExitNoFlyRegion(player);
        }
        
        // Update player's last region status
        lastRegionAllowFlight.put(uuid, canFly);
    }
    
    /**
     * Handle when a player enters a no-fly region
     * @param player The player
     */
    private void handleEnterNoFlyRegion(Player player) {
        // Disable flight but keep the player's flight state in our system
        player.setAllowFlight(false);
        player.setFlying(false);
        
        // Send message if configured to do so
        if (plugin.getConfig().getBoolean("worldguard.notify-region-change", true)) {
            String message = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("worldguard.no-fly-region-enter-message"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.prefix") + message));
        }
    }
    
    /**
     * Handle when a player exits a no-fly region
     * @param player The player
     */
    @SuppressWarnings("deprecation")
    private void handleExitNoFlyRegion(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Only re-enable flight if the player still has TempFly enabled in our system
        if (plugin.getPlayerDataManager().isFlightEnabled(uuid) && 
            plugin.getPlayerDataManager().getRemainingFlightTime(uuid) > 0) {
            
            // Re-enable flight
            player.setAllowFlight(true);
            player.setFlying(true);
            
            // Send message if configured to do so
            if (plugin.getConfig().getBoolean("worldguard.notify-region-change", true)) {
                String message = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("worldguard.no-fly-region-exit-message"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfig().getString("messages.prefix") + message));
            }
        }
    }
    
    /**
     * Check if a player is in a fly-allowed region
     * @param player The player
     * @return true if in a fly-allowed region
     */
    public boolean isInFlyAllowedRegion(Player player) {
        return canFlyAtLocation(player);
    }
    
    /**
     * Check if WorldGuard integration is enabled
     * @return true if enabled
     */
    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }
    
    /**
     * Remove player from region tracking when they leave
     * @param uuid Player UUID
     */
    public void removePlayer(UUID uuid) {
        lastRegionAllowFlight.remove(uuid);
    }
}