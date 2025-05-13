package id.nusatempfly;

import id.nusatempfly.commands.TempFlyCommand;
import id.nusatempfly.data.PlayerDataManager;
import id.nusatempfly.flight.FlightManager;
import id.nusatempfly.hooks.WorldGuardHook;
import id.nusatempfly.listeners.PlayerConnectionListener;
import id.nusatempfly.listeners.FlightToggleListener;
import id.nusatempfly.listeners.PlayerWorldChangeListener;
import id.nusatempfly.placeholders.TempFlyPlaceholders;
import id.nusatempfly.hooks.WorldGuardFlags;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

public class Plugin extends JavaPlugin {
    static {
        // This ensures our flag gets registered during WorldGuard's initialization
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            WorldGuardFlags.registerFlags();
        }
    }

    private static Plugin instance;
    private static final Logger LOGGER = Logger.getLogger("nusatempfly");
    
    private PlayerDataManager playerDataManager;
    private FlightManager flightManager;
    private WorldGuardHook worldGuardHook;
    
    @Override
    public void onEnable() {
        // Set instance
        instance = this;
        
        // Initialize config
        saveDefaultConfig();
        
        // Initialize data folder
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // Create player data directory
        File playerDataFolder = new File(getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
        
        // Initialize managers
        playerDataManager = new PlayerDataManager(this);
        flightManager = new FlightManager(this);
        
        // Initialize WorldGuard integration
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardHook = new WorldGuardHook(this);
            flightManager.setWorldGuardHook(worldGuardHook);
            LOGGER.info("WorldGuard hook initialized");
        }
        
        // Register commands
        getCommand("tempfly").setExecutor(new TempFlyCommand(this));
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new FlightToggleListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerWorldChangeListener(this), this);
        
        // Register PlaceholderAPI expansion if available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TempFlyPlaceholders(this).register();
            LOGGER.info("Hooked into PlaceholderAPI");
        }
        
        // Start flight time task (runs every second to decrement flight time)
        flightManager.startFlightTimeTask();
        
        LOGGER.info("NusaTempFly enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Save all player data when plugin disables
        if (playerDataManager != null) {
            playerDataManager.saveAllPlayerData();
        }
        
        // Cancel all tasks
        if (flightManager != null) {
            flightManager.stopFlightTimeTask();
        }
        
        LOGGER.info("NusaTempFly disabled successfully!");
    }
    
    /**
     * Get the plugin instance
     * @return Plugin instance
     */
    public static Plugin getInstance() {
        return instance;
    }
    
    /**
     * Get the player data manager
     * @return PlayerDataManager
     */
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    /**
     * Get the flight manager
     * @return FlightManager
     */
    public FlightManager getFlightManager() {
        return flightManager;
    }
    
    /**
     * Get the WorldGuard hook
     * @return WorldGuardHook
     */
    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }
}
