package xyz.rishabhvenu.abilityengine.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe storage for ability-scoped per-player state.
 * State is keyed by (abilityId, playerId, stateKey).
 * Automatically cleaned up on player quit or ability unload.
 */
public final class AbilityStateStore {
    
    // Map: "abilityId:playerUUID" -> Map<stateKey, value>
    private final Map<String, Map<String, Object>> store = new ConcurrentHashMap<>();
    
    /**
     * Stores a value for a specific ability, player, and key.
     * 
     * @param abilityId The ability ID
     * @param playerId The player UUID
     * @param key The state key
     * @param value The value to store
     */
    public void set(String abilityId, UUID playerId, String key, Object value) {
        if (abilityId == null || playerId == null || key == null) {
            throw new IllegalArgumentException("abilityId, playerId, and key must not be null");
        }
        
        String storeKey = makeStoreKey(abilityId, playerId);
        store.computeIfAbsent(storeKey, k -> new ConcurrentHashMap<>())
             .put(key, value);
    }
    
    /**
     * Retrieves a value for a specific ability, player, and key.
     * 
     * @param abilityId The ability ID
     * @param playerId The player UUID
     * @param key The state key
     * @return The stored value, or null if not found
     */
    public Object get(String abilityId, UUID playerId, String key) {
        if (abilityId == null || playerId == null || key == null) {
            return null;
        }
        
        String storeKey = makeStoreKey(abilityId, playerId);
        Map<String, Object> playerState = store.get(storeKey);
        return playerState != null ? playerState.get(key) : null;
    }
    
    /**
     * Clears all state for a specific ability and player.
     * 
     * @param abilityId The ability ID
     * @param playerId The player UUID
     */
    public void clear(String abilityId, UUID playerId) {
        if (abilityId == null || playerId == null) {
            return;
        }
        
        String storeKey = makeStoreKey(abilityId, playerId);
        store.remove(storeKey);
    }
    
    /**
     * Clears all state for a specific player across all abilities.
     * Called on player quit.
     * 
     * @param playerId The player UUID
     */
    public void clearPlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }
        
        String playerSuffix = ":" + playerId.toString();
        store.keySet().removeIf(key -> key.endsWith(playerSuffix));
    }
    
    /**
     * Clears all state for a specific ability across all players.
     * Called on ability unload (script reload).
     * 
     * @param abilityId The ability ID
     */
    public void clearAbility(String abilityId) {
        if (abilityId == null) {
            return;
        }
        
        String abilityPrefix = abilityId + ":";
        store.keySet().removeIf(key -> key.startsWith(abilityPrefix));
    }
    
    private String makeStoreKey(String abilityId, UUID playerId) {
        return abilityId + ":" + playerId.toString();
    }
}
