package xyz.rishabhvenu.abilityengine.config;

import xyz.rishabhvenu.abilityengine.api.AbilityContext;

import java.util.Map;

/**
 * Executes an action defined in configuration.
 */
public interface ActionExecutor {
    /**
     * Executes the action.
     * 
     * @param context The ability context
     * @param params Action parameters from config
     */
    void execute(AbilityContext context, Map<String, Object> params);
}
