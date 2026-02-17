package xyz.rishabhvenu.abilityengine.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime state for a single phase execution within an ability.
 * Tracks tick counter, phase-scoped state, and current phase name.
 */
public final class PhaseInstance {
    
    private String currentPhaseName;
    private int tickCount = 0;
    private final Map<String, Object> state = new HashMap<>();
    
    public PhaseInstance(String initialPhaseName) {
        this.currentPhaseName = initialPhaseName;
    }
    
    /**
     * Gets the current phase name.
     */
    public String getCurrentPhaseName() {
        return currentPhaseName;
    }
    
    /**
     * Sets the current phase name (when transitioning).
     */
    public void setCurrentPhaseName(String phaseName) {
        this.currentPhaseName = phaseName;
        this.tickCount = 0; // Reset tick count on phase transition
    }
    
    /**
     * Gets the tick count for the current phase.
     */
    public int getTickCount() {
        return tickCount;
    }
    
    /**
     * Increments the tick count.
     */
    public void incrementTick() {
        tickCount++;
    }
    
    /**
     * Gets a state value.
     */
    public Object get(String key) {
        return state.get(key);
    }
    
    /**
     * Sets a state value.
     */
    public void set(String key, Object value) {
        state.put(key, value);
    }
    
    /**
     * Clears all state.
     */
    public void clearState() {
        state.clear();
    }
    
    /**
     * Gets all state (for debugging).
     */
    public Map<String, Object> getAllState() {
        return new HashMap<>(state);
    }
}
