package id.nusatempfly.listeners;

import id.nusatempfly.Plugin;
import id.nusatempfly.data.PlayerData;
import id.nusatempfly.hooks.WorldGuardHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.UUID;

/**
 * Listener for player connection events (join/quit)
 */
public class PlayerConnectionListener implements Listener {
    private final Plugin plugin;
    
    public PlayerConnectionListener(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load player data
        plugin.getPlayerDataManager().getPlayerData(player);
        
        // Restore flight state if needed
        plugin.getFlightManager().restoreFlightState(player);
        
        plugin.getLogger().info("Loaded flight data for " + player.getName());
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Save flight state
        plugin.getFlightManager().saveFlightState(player);
        
        // If player had flight enabled but has no time remaining, disable it
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData.isFlightEnabled() && !playerData.hasFlightTimeRemaining()) {
            playerData.setFlightEnabled(false);
        }
        
        // Clean up WorldGuard tracking
        WorldGuardHook worldGuardHook = plugin.getWorldGuardHook();
        if (worldGuardHook != null) {
            worldGuardHook.removePlayer(uuid);
        }
        
        // Save and unload player data
        plugin.getPlayerDataManager().savePlayerData(uuid);
        plugin.getPlayerDataManager().unloadPlayerData(uuid);
        
        plugin.getLogger().info("Saved and unloaded flight data for " + player.getName());
    }
}