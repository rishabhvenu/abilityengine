package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.graalvm.polyglot.Value;
import xyz.rishabhvenu.abilityengine.api.AbilityContext;
import xyz.rishabhvenu.abilityengine.core.ExecutionTracker;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a single execution instance of an ability.
 * Tracks all resources owned by this execution (tasks, frozen entities, phases, movement).
 * Automatically cleaned up on ability completion or interrupt.
 */
public final class AbilityExecutionInstance {
    
    private static final AtomicLong EXECUTION_ID_GENERATOR = new AtomicLong(0);
    
    private final long executionId;
    private final String abilityId;
    private final UUID playerId;
    private final Plugin plugin;
    private final ExecutionTracker executionTracker;
    private final AbilityContext context;
    private final Value onInterruptCallback;
    private final xyz.rishabhvenu.abilityengine.core.EntityControlManager entityControlManager;
    
    // Resource tracking
    private final List<Integer> ownedTasks = Collections.synchronizedList(new ArrayList<>());
    private final Set<UUID> frozenEntities = Collections.synchronizedSet(new HashSet<>());
    private Object activePhase; // PhaseInstance reference
    private boolean cancelled = false;
    
    public AbilityExecutionInstance(
            String abilityId,
            UUID playerId,
            Plugin plugin,
            ExecutionTracker executionTracker,
            AbilityContext context,
            Value onInterruptCallback,
            xyz.rishabhvenu.abilityengine.core.EntityControlManager entityControlManager) {
        this.executionId = EXECUTION_ID_GENERATOR.incrementAndGet();
        this.abilityId = abilityId;
        this.playerId = playerId;
        this.plugin = plugin;
        this.executionTracker = executionTracker;
        this.context = context;
        this.onInterruptCallback = onInterruptCallback;
        this.entityControlManager = entityControlManager;
        
        // Register with tracker
        executionTracker.registerExecution(playerId, this);
    }
    
    public long getExecutionId() {
        return executionId;
    }
    
    public String getAbilityId() {
        return abilityId;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * Tracks a task owned by this execution.
     * 
     * @param taskId The Bukkit task ID
     */
    public void trackTask(int taskId) {
        ownedTasks.add(taskId);
    }
    
    /**
     * Tracks a frozen entity owned by this execution.
     * 
     * @param entityId The entity UUID
     */
    public void trackFrozenEntity(UUID entityId) {
        frozenEntities.add(entityId);
    }
    
    /**
     * Removes a frozen entity from tracking.
     * 
     * @param entityId The entity UUID
     */
    public void removeFrozenEntity(UUID entityId) {
        frozenEntities.remove(entityId);
    }
    
    /**
     * Gets all frozen entities owned by this execution.
     * 
     * @return Set of entity UUIDs
     */
    public Set<UUID> getFrozenEntities() {
        return new HashSet<>(frozenEntities);
    }
    
    /**
     * Sets the active phase instance.
     * 
     * @param phase The phase instance
     */
    public void setActivePhase(Object phase) {
        this.activePhase = phase;
    }
    
    /**
     * Gets the active phase instance.
     * 
     * @return The phase instance or null
     */
    public Object getActivePhase() {
        return activePhase;
    }
    
    /**
     * Cancels this execution instance.
     * - Cancels all owned tasks
     * - Unfreezes all owned entities
     * - Calls onInterrupt callback if present
     * - Unregisters from ExecutionTracker
     */
    public void cancel() {
        if (cancelled) {
            return;
        }
        cancelled = true;
        
        // Cancel all owned tasks
        synchronized (ownedTasks) {
            for (Integer taskId : ownedTasks) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            ownedTasks.clear();
        }
        
        // Unfreeze all owned entities
        if (entityControlManager != null) {
            synchronized (frozenEntities) {
                for (UUID entityId : frozenEntities) {
                    entityControlManager.unfreeze(entityId);
                }
                frozenEntities.clear();
            }
        }
        
        // Phase cleanup happens via task cancellation
        activePhase = null;
        
        // Call onInterrupt callback if present
        if (onInterruptCallback != null && onInterruptCallback.canExecute()) {
            try {
                // Create a fresh execution context for the interrupt callback
                AbilityExecContext interruptCtx = new AbilityExecContext(
                    context,
                    abilityId,
                    null, // No script context needed for callback
                    null, // No state store needed for callback
                    plugin,
                    null, // No cooldown manager
                    null, // No boss bar manager
                    this
                );
                onInterruptCallback.execute(interruptCtx);
            } catch (Exception e) {
                plugin.getLogger().warning("Error executing onInterrupt callback for " + abilityId + ": " + e.getMessage());
            }
        }
        
        // Unregister from tracker
        executionTracker.unregisterExecution(playerId, this);
    }
    
    /**
     * Completes this execution normally (not cancelled).
     * Cleans up resources without calling onInterrupt.
     */
    public void complete() {
        if (cancelled) {
            return;
        }
        
        // Unregister from tracker without calling cancel
        executionTracker.unregisterExecution(playerId, this);
    }
}
