package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.entity.Entity;
import org.graalvm.polyglot.Value;
import xyz.rishabhvenu.abilityengine.core.EntityControlManager;

/**
 * Provides entity control utilities for scripts.
 * Exposed as engine.control
 */
public final class ControlBindings {
    
    private final EntityControlManager entityControlManager;
    
    public ControlBindings(EntityControlManager entityControlManager) {
        this.entityControlManager = entityControlManager;
    }
    
    /**
     * Freezes an entity.
     * 
     * @param entity The entity to freeze
     * @param config JS object with duration, preventMovement, preventRotation
     * @param execution The execution instance for tracking
     */
    public void freeze(Entity entity, Value config, AbilityExecutionInstance execution) {
        boolean preventMovement = true;
        boolean preventRotation = false;
        int duration = 0;
        
        if (config != null && config.hasMembers()) {
            if (config.hasMember("preventMovement") && config.getMember("preventMovement").isBoolean()) {
                preventMovement = config.getMember("preventMovement").asBoolean();
            }
            if (config.hasMember("preventRotation") && config.getMember("preventRotation").isBoolean()) {
                preventRotation = config.getMember("preventRotation").asBoolean();
            }
            if (config.hasMember("duration") && config.getMember("duration").isNumber()) {
                duration = config.getMember("duration").asInt();
            }
        }
        
        entityControlManager.freeze(entity.getUniqueId(), preventMovement, preventRotation, duration);
        execution.trackFrozenEntity(entity.getUniqueId());
    }
    
    /**
     * Unfreezes an entity.
     * 
     * @param entity The entity to unfreeze
     * @param execution The execution instance for tracking
     */
    public void unfreeze(Entity entity, AbilityExecutionInstance execution) {
        entityControlManager.unfreeze(entity.getUniqueId());
        execution.removeFrozenEntity(entity.getUniqueId());
    }
    
    /**
     * Checks if an entity is frozen.
     * 
     * @param entity The entity to check
     * @return true if frozen
     */
    public boolean isFrozen(Entity entity) {
        return entityControlManager.isFrozen(entity.getUniqueId());
    }
}
