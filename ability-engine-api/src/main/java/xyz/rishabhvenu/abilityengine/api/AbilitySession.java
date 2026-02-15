package xyz.rishabhvenu.abilityengine.api;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Represents an active ability session for a player.
 * Sessions support abilities that have continuous effects or need to track state.
 */
public interface AbilitySession {
    /**
     * Returns the unique identifier for this session.
     * 
     * @return Session ID
     */
    UUID sessionId();
    
    /**
     * Returns the player this session is bound to.
     * 
     * @return The player
     */
    Player player();
    
    /**
     * Returns the ability this session is running.
     * 
     * @return The ability
     */
    Ability ability();
    
    /**
     * Called when the session starts.
     */
    void start();
    
    /**
     * Called every tick while the session is active.
     */
    void tick();
    
    /**
     * Called when the session ends.
     * Implementations should clean up any resources.
     */
    void end();
    
    /**
     * Checks if this session is currently active.
     * 
     * @return true if active
     */
    boolean isActive();
    
    /**
     * Gets the number of ticks this session has been active.
     * 
     * @return Tick count
     */
    int getTickCount();
}
