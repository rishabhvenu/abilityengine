package xyz.rishabhvenu.abilityengine.core;

import xyz.rishabhvenu.abilityengine.api.CooldownManager;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of CooldownManager with automatic expiry checking.
 * Thread-safe using ConcurrentHashMap.
 */
public final class CooldownManagerImpl implements CooldownManager {
    
    // Map: PlayerId -> (AbilityId -> ExpiryTimeMillis)
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    
    @Override
    public boolean isReady(UUID playerId, String abilityId) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return true;
        }
        
        Long expiryTime = playerCooldowns.get(abilityId);
        if (expiryTime == null) {
            return true;
        }
        
        long now = System.currentTimeMillis();
        if (now >= expiryTime) {
            // Cooldown expired, clean it up
            playerCooldowns.remove(abilityId);
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(playerId);
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    public void setCooldown(UUID playerId, String abilityId, Duration duration) {
        if (duration.isZero() || duration.isNegative()) {
            // No cooldown or invalid duration, don't store
            return;
        }
        
        long expiryTime = System.currentTimeMillis() + duration.toMillis();
        cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                 .put(abilityId, expiryTime);
    }
    
    @Override
    public Duration getRemainingCooldown(UUID playerId, String abilityId) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return Duration.ZERO;
        }
        
        Long expiryTime = playerCooldowns.get(abilityId);
        if (expiryTime == null) {
            return Duration.ZERO;
        }
        
        long now = System.currentTimeMillis();
        long remaining = expiryTime - now;
        
        if (remaining <= 0) {
            // Expired, clean up
            playerCooldowns.remove(abilityId);
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(playerId);
            }
            return Duration.ZERO;
        }
        
        return Duration.ofMillis(remaining);
    }
    
    @Override
    public void clearCooldowns(UUID playerId) {
        cooldowns.remove(playerId);
    }
}
