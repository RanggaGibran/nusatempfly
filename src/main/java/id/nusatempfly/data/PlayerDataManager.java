package id.nusatempfly.data;

import id.nusatempfly.Plugin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerDataManager {
    private final Plugin plugin;
    private final Map<UUID, PlayerData> playerDataMap;
    private final File playerDataFolder;
    
    public PlayerDataManager(Plugin plugin) {
        this.plugin = plugin;
        this.playerDataMap = new HashMap<>();
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }
    
    /**
     * Get player data for a player
     * @param player The player
     * @return PlayerData instance
     */
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }
    
    /**
     * Get player data by UUID
     * @param uuid Player UUID
     * @return PlayerData instance
     */
    public PlayerData getPlayerData(UUID uuid) {
        if (!playerDataMap.containsKey(uuid)) {
            loadPlayerData(uuid);
        }
        return playerDataMap.get(uuid);
    }
    
    /**
     * Load player data from file
     * @param uuid Player UUID
     */
    public void loadPlayerData(UUID uuid) {
        File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
        PlayerData playerData = new PlayerData(uuid);
        
        if (playerFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            playerData.setRemainingFlightTime(config.getLong("remainingFlightTime", 0));
            playerData.setFlightEnabled(config.getBoolean("flightEnabled", false));
            plugin.getLogger().info("Loaded player data for " + uuid);
        }
        
        playerDataMap.put(uuid, playerData);
    }
    
    /**
     * Save player data to file
     * @param uuid Player UUID
     */
    public void savePlayerData(UUID uuid) {
        if (!playerDataMap.containsKey(uuid)) {
            return;
        }
        
        PlayerData playerData = playerDataMap.get(uuid);
        File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        
        config.set("remainingFlightTime", playerData.getRemainingFlightTime());
        config.set("flightEnabled", playerData.isFlightEnabled());
        
        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + uuid, e);
        }
    }
    
    /**
     * Save all player data
     */
    public void saveAllPlayerData() {
        for (UUID uuid : playerDataMap.keySet()) {
            savePlayerData(uuid);
        }
        plugin.getLogger().info("Saved all player data");
    }
    
    /**
     * Unload player data from memory
     * @param uuid Player UUID
     */
    public void unloadPlayerData(UUID uuid) {
        if (playerDataMap.containsKey(uuid)) {
            savePlayerData(uuid);
            playerDataMap.remove(uuid);
        }
    }
    
    /**
     * Add flight time to player
     * @param uuid Player UUID
     * @param seconds Seconds to add
     * @return The total time after adding
     */
    public long addFlightTime(UUID uuid, long seconds) {
        PlayerData playerData = getPlayerData(uuid);
        long newTime = playerData.getRemainingFlightTime() + seconds;
        playerData.setRemainingFlightTime(newTime);
        return newTime;
    }
    
    /**
     * Remove flight time from player
     * @param uuid Player UUID
     * @param seconds Seconds to remove
     * @return The total time after removing
     */
    public long removeFlightTime(UUID uuid, long seconds) {
        PlayerData playerData = getPlayerData(uuid);
        long newTime = Math.max(0, playerData.getRemainingFlightTime() - seconds);
        playerData.setRemainingFlightTime(newTime);
        return newTime;
    }
    
    /**
     * Set flight time for player
     * @param uuid Player UUID
     * @param seconds Seconds to set
     */
    public void setFlightTime(UUID uuid, long seconds) {
        PlayerData playerData = getPlayerData(uuid);
        playerData.setRemainingFlightTime(seconds);
    }
    
    /**
     * Get remaining flight time for player
     * @param uuid Player UUID
     * @return Remaining flight time in seconds
     */
    public long getRemainingFlightTime(UUID uuid) {
        return getPlayerData(uuid).getRemainingFlightTime();
    }
    
    /**
     * Set flight enabled state for player
     * @param uuid Player UUID
     * @param enabled Whether flight is enabled
     */
    public void setFlightEnabled(UUID uuid, boolean enabled) {
        getPlayerData(uuid).setFlightEnabled(enabled);
    }
    
    /**
     * Check if flight is enabled for player
     * @param uuid Player UUID
     * @return Whether flight is enabled
     */
    public boolean isFlightEnabled(UUID uuid) {
        return getPlayerData(uuid).isFlightEnabled();
    }
}