package id.nusatempfly.listeners;

import id.nusatempfly.Plugin;
import id.nusatempfly.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

/**
 * Listener for flight-related events
 */
public class FlightToggleListener implements Listener {
    private final Plugin plugin;
    
    public FlightToggleListener(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        
        // If toggling flight on
        if (event.isFlying()) {
            // Check if player has permission to use
            if (!player.hasPermission("nusatempfly.use")) {
                return; // Allow default behavior
            }
            
            // Check if player has flight time or bypass permission
            if (!playerData.hasFlightTimeRemaining() && !player.hasPermission("nusatempfly.bypass.timelimit")) {
                event.setCancelled(true);
                String prefix = ChatColor.translateAlternateColorCodes('&', 
                        plugin.getConfig().getString("messages.prefix"));
                player.sendMessage(prefix + ChatColor.RED + "You don't have any flight time remaining!");
                return;
            }
            
            // If we get here, player can use flight
            playerData.setFlightEnabled(true);
        } 
        // If toggling flight off
        else {
            // Only manage our own flight states
            if (playerData.isFlightEnabled()) {
                playerData.setFlightEnabled(false);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDamage(EntityDamageEvent event) {
        // Check if configured to disable flight on damage
        if (!plugin.getConfig().getBoolean("flight.disable-on-damage")) {
            return;
        }
        
        // Check if entity is a player
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Skip if player has bypass permission
        if (player.hasPermission("nusatempfly.bypass.damage")) {
            return;
        }
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        
        // Check if player has flight enabled by our plugin
        if (!playerData.isFlightEnabled()) {
            return;
        }
        
        // Disable flight due to damage
        plugin.getFlightManager().disableFlight(player);
        
        // Send message
        String prefix = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.prefix"));
        player.sendMessage(prefix + ChatColor.RED + "Your flight has been disabled due to taking damage!");
    }
}