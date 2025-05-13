package id.nusatempfly.data;

import java.util.UUID;

public class PlayerData {
    private final UUID playerUUID;
    private long remainingFlightTime;
    private boolean flightEnabled;
    
    public PlayerData(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.remainingFlightTime = 0;
        this.flightEnabled = false;
    }
    
    /**
     * Get player UUID
     * @return Player UUID
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    /**
     * Get remaining flight time in seconds
     * @return Remaining flight time
     */
    public long getRemainingFlightTime() {
        return remainingFlightTime;
    }
    
    /**
     * Set remaining flight time in seconds
     * @param remainingFlightTime Time in seconds
     */
    public void setRemainingFlightTime(long remainingFlightTime) {
        this.remainingFlightTime = Math.max(0, remainingFlightTime);
    }
    
    /**
     * Check if flight is enabled
     * @return true if flight is enabled
     */
    public boolean isFlightEnabled() {
        return flightEnabled;
    }
    
    /**
     * Set flight enabled state
     * @param flightEnabled New flight state
     */
    public void setFlightEnabled(boolean flightEnabled) {
        this.flightEnabled = flightEnabled;
    }
    
    /**
     * Reduce remaining flight time by specified seconds
     * @param seconds Seconds to reduce
     * @return true if time is still remaining, false if time expired
     */
    public boolean reduceFlightTime(long seconds) {
        remainingFlightTime = Math.max(0, remainingFlightTime - seconds);
        return remainingFlightTime > 0;
    }
    
    /**
     * Check if player has any flight time remaining
     * @return true if has time remaining
     */
    public boolean hasFlightTimeRemaining() {
        return remainingFlightTime > 0;
    }
}