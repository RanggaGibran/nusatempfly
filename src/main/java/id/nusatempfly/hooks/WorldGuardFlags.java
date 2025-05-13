package id.nusatempfly.hooks;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.session.SessionManager;
import id.nusatempfly.Plugin;
import org.bukkit.Bukkit;

/**
 * Class to register WorldGuard flags at the appropriate time
 */
public class WorldGuardFlags {
    
    // The custom flag we want to register
    public static StateFlag TEMP_FLY_ALLOWED;
    
    /**
     * This method should be called during server startup
     * It's separate from the plugin's enable method
     */
    public static void registerFlags() {
        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            
            // Create our custom flag (permissions can be used to determine who can change the flag)
            StateFlag flag = new StateFlag("tempfly-allowed", true);
            
            // Register the flag with WorldGuard
            registry.register(flag);
            TEMP_FLY_ALLOWED = flag;
            
            Bukkit.getLogger().info("NusaTempFly: Successfully registered WorldGuard flags");
        } catch (FlagConflictException e) {
            // If the flag already exists, let's use the existing one
            Bukkit.getLogger().info("NusaTempFly: WorldGuard flag tempfly-allowed already exists, using the existing one");
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            Flag<?> existing = registry.get("tempfly-allowed");
            if (existing instanceof StateFlag) {
                TEMP_FLY_ALLOWED = (StateFlag) existing;
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("NusaTempFly: Failed to register WorldGuard flag: " + e.getMessage());
        }
    }
}