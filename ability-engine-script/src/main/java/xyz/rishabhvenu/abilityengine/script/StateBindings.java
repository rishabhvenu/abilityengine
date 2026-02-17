package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.entity.Player;
import xyz.rishabhvenu.abilityengine.core.AbilityStateStore;

/**
 * Provides state management API for scripts.
 * Exposed as engine.state
 */
public final class StateBindings {
    
    private final AbilityStateStore stateStore;
    
    StateBindings(AbilityStateStore stateStore) {
        this.stateStore = stateStore;
    }
    
    /**
     * Sets a state value for a specific ability and player.
     * 
     * @param player The player
     * @param abilityId The ability ID
     * @param key The state key
     * @param value The value to store
     */
    public void set(Player player, String abilityId, String key, Object value) {
        stateStore.set(abilityId, player.getUniqueId(), key, value);
    }
    
    /**
     * Gets a state value for a specific ability and player.
     * 
     * @param player The player
     * @param abilityId The ability ID
     * @param key The state key
     * @return The stored value, or null if not found
     */
    public Object get(Player player, String abilityId, String key) {
        return stateStore.get(abilityId, player.getUniqueId(), key);
    }
    
    /**
     * Clears all state for a specific ability and player.
     * 
     * @param player The player
     * @param abilityId The ability ID
     */
    public void clear(Player player, String abilityId) {
        stateStore.clear(abilityId, player.getUniqueId());
    }
}
