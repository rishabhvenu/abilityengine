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
    JUMP,
    LAND,
    
    // Advanced triggers
    DOUBLE_SHIFT,
    HOLD_SHIFT,
    
    // Projectile triggers
    PROJECTILE_HIT,
    
    // Combat triggers (extended)
    KILL_ENTITY,
    
    // Lifecycle triggers
    ON_JOIN,
    ON_QUIT,
    
    // Tick-based trigger (fires every tick for active sessions)
    TICK,
    
    // Custom event trigger
    CUSTOM
}
