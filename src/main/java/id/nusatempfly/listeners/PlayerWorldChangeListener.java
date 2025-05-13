package id.nusatempfly.listeners;

import id.nusatempfly.Plugin;
import id.nusatempfly.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

/**
 * Listener untuk menangani perpindahan dunia player
 */
public class PlayerWorldChangeListener implements Listener {
    private final Plugin plugin;
    
    public PlayerWorldChangeListener(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        
        // Sinkronkan status terbang saat pemain pindah dunia
        // Bug terjadi karena status terbang player tidak disinkronkan saat pindah dunia
        plugin.getFlightManager().synchronizeFlightState(player);
    }
}