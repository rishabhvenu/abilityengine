package xyz.rishabhvenu.abilityengine.api;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * Represents an ability that can be triggered and executed.
 */
public interface Ability {
    /**
     * Returns the unique identifier for this ability.
     * 
     * @return The ability ID (must be unique across all abilities)
     */
    String id();
    
    /**
     * Returns the trigger types that can activate this ability.
     * 
     * @return Collection of trigger types
     */
    Collection<TriggerType> triggers();
    
    /**
     * Returns the conditions that must be met for this ability to execute.
     * All conditions are evaluated with AND logic.
     * 
     * @return List of conditions (empty list means no conditions)
     */
    List<Condition> conditions();
    
    /**
     * Executes the ability logic.
     * 
     * @param context The execution context containing player, trigger, etc.
     */
    void execute(AbilityContext context);
    
    /**
     * Returns the cooldown duration for this ability.
     * 
     * @return Cooldown duration, or Duration.ZERO for no cooldown
     */
    Duration cooldown();
}
