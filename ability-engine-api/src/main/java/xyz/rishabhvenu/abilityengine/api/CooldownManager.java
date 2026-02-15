package xyz.rishabhvenu.abilityengine.api;

import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.UUID;

/**
 * Manages per-player cooldowns for abilities.
 */
public interface CooldownManager {
    /**
     * Checks if an ability is ready (not on cooldown) for a player.
     * 
     * @param playerId The player's UUID
     * @param abilityId The ability ID
     * @return true if the ability is ready to use
     */
    boolean isReady(UUID playerId, String abilityId);
    
    /**
     * Sets a cooldown for an ability for a player.
     * 
     * @param playerId The player's UUID
     * @param abilityId The ability ID
     * @param duration The cooldown duration
     */
    void setCooldown(UUID playerId, String abilityId, Duration duration);
    
    /**
     * Gets the remaining cooldown time for an ability.
     * 
     * @param playerId The player's UUID
     * @param abilityId The ability ID
     * @return Remaining cooldown duration, or Duration.ZERO if ready
     */
    Duration getRemainingCooldown(UUID playerId, String abilityId);
    
    /**
     * Clears all cooldowns for a player.
     * 
     * @param playerId The player's UUID
     */
    void clearCooldowns(UUID playerId);
    
    /**
     * Convenience method to check cooldown for a player.
     * 
     * @param player The player
     * @param abilityId The ability ID
     * @return true if the ability is ready to use
     */
    default boolean isReady(Player player, String abilityId) {
        return isReady(player.getUniqueId(), abilityId);
    }
    
    /**
     * Convenience method to set cooldown for a player.
     * 
     * @param player The player
     * @param abilityId The ability ID
     * @param duration The cooldown duration
     */
    default void setCooldown(Player player, String abilityId, Duration duration) {
        setCooldown(player.getUniqueId(), abilityId, duration);
    }
}
