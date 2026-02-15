package xyz.rishabhvenu.abilityengine.api;

/**
 * Represents a type of trigger that can activate an ability.
 * Includes both standard interaction triggers and special triggers.
 */
public enum TriggerType {
    // Standard click interactions
    RIGHT_CLICK,
    LEFT_CLICK,
    
    // Shift + click interactions
    SHIFT_RIGHT_CLICK,
    SHIFT_LEFT_CLICK,
    
    // Entity interaction
    LEFT_CLICK_ENTITY,
    RIGHT_CLICK_ENTITY,
    SHIFT_LEFT_CLICK_ENTITY,
    SHIFT_RIGHT_CLICK_ENTITY,
    
    // Combat triggers
    DAMAGE_DEALT,
    DAMAGE_TAKEN,
    
    // Movement triggers
    MOVE,
    
    // Tick-based trigger (fires every tick for active sessions)
    TICK
}
