package xyz.rishabhvenu.abilityengine.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active ability executions per player.
 * Used by the interrupt system to find and cancel executions.
 * Thread-safe using ConcurrentHashMap.
 */
public final class ExecutionTracker {
    
    // Map: PlayerId -> List of active execution instances
    private final Map<UUID, List<Object>> activeExecutions = new ConcurrentHashMap<>();
    
    /**
     * Registers an active execution instance for a player.
     * 
     * @param playerId The player's UUID
     * @param execution The execution instance (must have cancel() method)
     */
    public void registerExecution(UUID playerId, Object execution) {
        activeExecutions.computeIfAbsent(playerId, k -> Collections.synchronizedList(new ArrayList<>()))
                       .add(execution);
    }
    
    /**
     * Unregisters an execution instance.
     * 
     * @param playerId The player's UUID
     * @param execution The execution instance
     */
    public void unregisterExecution(UUID playerId, Object execution) {
        List<Object> executions = activeExecutions.get(playerId);
        if (executions != null) {
            executions.remove(execution);
            if (executions.isEmpty()) {
                activeExecutions.remove(playerId);
            }
        }
    }
    
    /**
     * Gets all active executions for a player.
     * Returns a copy to prevent concurrent modification.
     * 
     * @param playerId The player's UUID
     * @return List of active execution instances
     */
    public List<Object> getExecutions(UUID playerId) {
        List<Object> executions = activeExecutions.get(playerId);
        if (executions == null) {
            return Collections.emptyList();
        }
        synchronized (executions) {
            return new ArrayList<>(executions);
        }
    }
    
    /**
     * Clears all executions for a player.
     * Called on player quit.
     * 
     * @param playerId The player's UUID
     */
    public void clearPlayer(UUID playerId) {
        activeExecutions.remove(playerId);
    }
    
    /**
     * Gets the total number of active executions across all players.
     * 
     * @return Total execution count
     */
    public int getTotalExecutions() {
        return activeExecutions.values().stream()
            .mapToInt(List::size)
            .sum();
    }
}
