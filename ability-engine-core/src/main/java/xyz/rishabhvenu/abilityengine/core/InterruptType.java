package xyz.rishabhvenu.abilityengine.core;

/**
 * Represents types of interrupts that can cancel ability executions.
 */
public enum InterruptType {
    /**
     * Triggered when the player takes damage.
     */
    TAKE_DAMAGE,
    
    /**
     * Triggered when the player switches their held item.
     */
    SWITCH_ITEM,
    
    /**
     * Triggered when the player dies.
     */
    DEATH,
    
    /**
     * Triggered when the player quits the server.
     */
    QUIT
}
